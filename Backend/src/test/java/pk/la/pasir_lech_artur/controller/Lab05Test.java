package pk.la.pasir_lech_artur.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import pk.la.pasir_lech_artur.dto.UserDTO;
import pk.la.pasir_lech_artur.repository.*;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Lab05Test {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private DebtRepository debtRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PASSWORD = "Password123";

    record TestUser(Long id, String email, String token) {}

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        debtRepository.deleteAll();
        membershipRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String uniqueEmail() {
        return "test_" + UUID.randomUUID().toString().substring(0, 8) + "@pk.pl";
    }

    private TestUser registerAndLogin(String username) throws Exception {
        String email = uniqueEmail();

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(username);
        userDTO.setEmail(email);
        userDTO.setPassword(PASSWORD);

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(registerResponse).get("id").asLong();

        String loginJson = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, PASSWORD);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();

        return new TestUser(id, email, token);
    }

    private JsonNode graphql(String token, String query) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("query", query));

        String response = mockMvc.perform(post("/graphql")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();

        System.out.println(response);

        return objectMapper.readTree(response);
    }

    private JsonNode createGroup(TestUser user, String name) throws Exception {
        return graphql(user.token(), """
                mutation {
                  createGroup(groupDTO: { name: "%s" }) {
                    id
                    name
                    ownerId
                  }
                }
                """.formatted(name));
    }

    private void addMember(TestUser owner, String groupId, TestUser member) throws Exception {
        graphql(owner.token(), """
                mutation {
                  addMember(membershipDTO: {
                    userEmail: "%s",
                    groupId: "%s"
                  }) {
                    id
                    userId
                    groupId
                    userEmail
                  }
                }
                """.formatted(member.email(), groupId));
    }

    private JsonNode createDebt(TestUser user, String groupId, Long debtorId, Long creditorId) throws Exception {
        return graphql(user.token(), """
                mutation {
                  createDebt(debtDTO: {
                    debtorId: "%s",
                    creditorId: "%s",
                    groupId: "%s",
                    amount: 100.0,
                    title: "Test debt"
                  }) {
                    id
                    amount
                    title
                    debtor { id }
                    creditor { id }
                    group { id }
                  }
                }
                """.formatted(debtorId, creditorId, groupId));
    }

    @Test
    void shouldCreateGroupAddOwnerAsMemberAndReturnInMyGroups() throws Exception {
        TestUser owner = registerAndLogin("owner");

        createGroup(owner, "Test group");

        JsonNode result = graphql(owner.token(), """
                query {
                  myGroups {
                    id
                    name
                    ownerId
                  }
                }
                """);

        assertEquals("Test group", result.get("data").get("myGroups").get(0).get("name").asText());
        assertEquals(owner.id().toString(), result.get("data").get("myGroups").get(0).get("ownerId").asText());
    }

    @Test
    void shouldAllowOnlyOwnerToAddMembers() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser member = registerAndLogin("member");
        TestUser outsider = registerAndLogin("outsider");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();
        addMember(owner, groupId, member);

        JsonNode result = graphql(member.token(), """
                mutation {
                  addMember(membershipDTO: {
                    userEmail: "%s",
                    groupId: "%s"
                  }) {
                    id
                  }
                }
                """.formatted(outsider.email(), groupId));

        assertTrue(result.has("errors"));
    }

    @Test
    void shouldReturnGroupMembersOnlyForGroupMember() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser outsider = registerAndLogin("outsider");

        String groupId = createGroup(owner, "Private group").get("data").get("createGroup").get("id").asText();

        JsonNode result = graphql(outsider.token(), """
                query {
                  groupMembers(groupId: "%s") {
                    id
                  }
                }
                """.formatted(groupId));

        assertTrue(result.has("errors"));
    }

    @Test
    void shouldReturnGroupDebtsOnlyForGroupMember() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser outsider = registerAndLogin("outsider");

        String groupId = createGroup(owner, "Debt group").get("data").get("createGroup").get("id").asText();

        JsonNode result = graphql(outsider.token(), """
                query {
                  groupDebts(groupId: "%s") {
                    id
                  }
                }
                """.formatted(groupId));

        assertTrue(result.has("errors"));
    }

    @Test
    void newMemberShouldGetOnlyDebtsFromTransactionsAfterJoining() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser member1 = registerAndLogin("member1");
        TestUser member2 = registerAndLogin("member2");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();
        addMember(owner, groupId, member1);

        graphql(owner.token(), """
                mutation {
                  addGroupTransaction(groupTransactionDTO: {
                    groupId: "%s",
                    amount: 90.0,
                    type: "EXPENSE",
                    title: "Before join"
                  })
                }
                """.formatted(groupId));

        addMember(owner, groupId, member2);

        graphql(owner.token(), """
                mutation {
                  addGroupTransaction(groupTransactionDTO: {
                    groupId: "%s",
                    amount: 90.0,
                    type: "EXPENSE",
                    title: "After join"
                  })
                }
                """.formatted(groupId));

        JsonNode debts = graphql(owner.token(), """
                query {
                  groupDebts(groupId: "%s") {
                    title
                    debtor { id }
                    creditor { id }
                  }
                }
                """.formatted(groupId));

        int member2Debts = 0;

        for (JsonNode debt : debts.get("data").get("groupDebts")) {
            String debtorId = debt.get("debtor").get("id").asText();
            String creditorId = debt.get("creditor").get("id").asText();

            if (debtorId.equals(member2.id().toString()) || creditorId.equals(member2.id().toString())) {
                member2Debts++;
                assertEquals("After join", debt.get("title").asText());
            }
        }

        assertEquals(1, member2Debts);
    }

    @Test
    void incomeGroupTransactionShouldCreateDebtsFromCurrentUserToOtherMembers() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser member = registerAndLogin("member");

        String groupId = createGroup(owner, "Income group").get("data").get("createGroup").get("id").asText();
        addMember(owner, groupId, member);

        graphql(owner.token(), """
                mutation {
                  addGroupTransaction(groupTransactionDTO: {
                    groupId: "%s",
                    amount: 100.0,
                    type: "INCOME",
                    title: "Income transaction"
                  })
                }
                """.formatted(groupId));

        JsonNode debts = graphql(owner.token(), """
                query {
                  groupDebts(groupId: "%s") {
                    debtor { id }
                    creditor { id }
                  }
                }
                """.formatted(groupId));

        JsonNode debt = debts.get("data").get("groupDebts").get(0);

        assertEquals(owner.id().toString(), debt.get("debtor").get("id").asText());
        assertEquals(member.id().toString(), debt.get("creditor").get("id").asText());
    }

    @Test
    void removingMemberShouldNotRemoveHistoricalDebts() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser member = registerAndLogin("member");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();
        addMember(owner, groupId, member);

        createDebt(owner, groupId, member.id(), owner.id());

        JsonNode members = graphql(owner.token(), """
                query {
                  groupMembers(groupId: "%s") {
                    id
                    userId
                  }
                }
                """.formatted(groupId));

        String membershipId = null;

        for (JsonNode membership : members.get("data").get("groupMembers")) {
            if (membership.get("userId").asText().equals(member.id().toString())) {
                membershipId = membership.get("id").asText();
            }
        }

        graphql(owner.token(), """
                mutation {
                  removeMember(membershipId: "%s")
                }
                """.formatted(membershipId));

        JsonNode debts = graphql(owner.token(), """
                query {
                  groupDebts(groupId: "%s") {
                    id
                  }
                }
                """.formatted(groupId));

        assertEquals(1, debts.get("data").get("groupDebts").size());
    }

    @Test
    void shouldNotRemoveOwnerByRemoveMember() throws Exception {
        TestUser owner = registerAndLogin("owner");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();

        JsonNode members = graphql(owner.token(), """
                query {
                  groupMembers(groupId: "%s") {
                    id
                    userId
                  }
                }
                """.formatted(groupId));

        String ownerMembershipId = members.get("data").get("groupMembers").get(0).get("id").asText();

        JsonNode result = graphql(owner.token(), """
                mutation {
                  removeMember(membershipId: "%s")
                }
                """.formatted(ownerMembershipId));

        assertTrue(result.has("errors"));
    }

    @Test
    void nonOwnerMemberShouldNotDeleteGroup() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser member = registerAndLogin("member");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();
        addMember(owner, groupId, member);

        JsonNode result = graphql(member.token(), """
                mutation {
                  deleteGroup(id: "%s")
                }
                """.formatted(groupId));

        assertTrue(result.has("errors"));
    }

    @Test
    void createDebtShouldCreateManualDebtOnlyBetweenMembersOfSameGroup() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser member = registerAndLogin("member");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();
        addMember(owner, groupId, member);

        JsonNode result = createDebt(owner, groupId, member.id(), owner.id());

        assertFalse(result.has("errors"));
        assertEquals(member.id().toString(), result.get("data").get("createDebt").get("debtor").get("id").asText());
        assertEquals(owner.id().toString(), result.get("data").get("createDebt").get("creditor").get("id").asText());
    }

    @Test
    void createDebtShouldRejectUserOutsideGroup() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser outsider = registerAndLogin("outsider");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();

        JsonNode result = createDebt(owner, groupId, outsider.id(), owner.id());

        assertTrue(result.has("errors"));
    }

    @Test
    void createDebtShouldRejectDebtToSameUser() throws Exception {
        TestUser owner = registerAndLogin("owner");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();

        JsonNode result = createDebt(owner, groupId, owner.id(), owner.id());

        assertTrue(result.has("errors"));
    }

    @Test
    void groupOwnerCanCreateDebtBetweenOtherMembers() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser member1 = registerAndLogin("member1");
        TestUser member2 = registerAndLogin("member2");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();
        addMember(owner, groupId, member1);
        addMember(owner, groupId, member2);

        JsonNode result = createDebt(owner, groupId, member1.id(), member2.id());

        assertFalse(result.has("errors"));
    }

    @Test
    void groupMemberCanCreateDebtOnlyWhenParticipant() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser member1 = registerAndLogin("member1");
        TestUser member2 = registerAndLogin("member2");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();
        addMember(owner, groupId, member1);
        addMember(owner, groupId, member2);

        JsonNode result = createDebt(member1, groupId, member1.id(), member2.id());

        assertFalse(result.has("errors"));
    }

    @Test
    void groupMemberCannotCreateDebtWhenNotParticipant() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser member1 = registerAndLogin("member1");
        TestUser member2 = registerAndLogin("member2");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();
        addMember(owner, groupId, member1);
        addMember(owner, groupId, member2);

        JsonNode result = createDebt(member1, groupId, owner.id(), member2.id());

        assertTrue(result.has("errors"));
    }

    @Test
    void deleteDebtShouldDeleteDebtAvailableForDebtParticipant() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser member = registerAndLogin("member");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();
        addMember(owner, groupId, member);

        String debtId = createDebt(owner, groupId, member.id(), owner.id())
                .get("data").get("createDebt").get("id").asText();

        JsonNode result = graphql(member.token(), """
                mutation {
                  deleteDebt(debtId: "%s")
                }
                """.formatted(debtId));

        assertFalse(result.has("errors"));
        assertTrue(result.get("data").get("deleteDebt").asBoolean());
    }

    @Test
    void deleteDebtShouldRejectMemberWhoIsNotOwnerOrParticipant() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser member1 = registerAndLogin("member1");
        TestUser member2 = registerAndLogin("member2");
        TestUser member3 = registerAndLogin("member3");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();
        addMember(owner, groupId, member1);
        addMember(owner, groupId, member2);
        addMember(owner, groupId, member3);

        String debtId = createDebt(owner, groupId, member1.id(), member2.id())
                .get("data").get("createDebt").get("id").asText();

        JsonNode result = graphql(member3.token(), """
                mutation {
                  deleteDebt(debtId: "%s")
                }
                """.formatted(debtId));

        assertTrue(result.has("errors"));
    }

    @Test
    void ownerCanDeleteDebtWhenNotParticipant() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser member1 = registerAndLogin("member1");
        TestUser member2 = registerAndLogin("member2");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();
        addMember(owner, groupId, member1);
        addMember(owner, groupId, member2);

        String debtId = createDebt(owner, groupId, member1.id(), member2.id())
                .get("data").get("createDebt").get("id").asText();

        JsonNode result = graphql(owner.token(), """
                mutation {
                  deleteDebt(debtId: "%s")
                }
                """.formatted(debtId));

        assertFalse(result.has("errors"));
        assertTrue(result.get("data").get("deleteDebt").asBoolean());
    }

    @Test
    void graphqlValidationShouldRejectEmptyOrInvalidValues() throws Exception {
        TestUser owner = registerAndLogin("owner");

        JsonNode result = graphql(owner.token(), """
                mutation {
                  createGroup(groupDTO: { name: "" }) {
                    id
                  }
                }
                """);

        assertTrue(result.has("errors"));
    }

    @Test
    void ownerDeletingGroupShouldDeleteRelatedDebtsAndGroup() throws Exception {
        TestUser owner = registerAndLogin("owner");
        TestUser member = registerAndLogin("member");

        String groupId = createGroup(owner, "Group").get("data").get("createGroup").get("id").asText();
        addMember(owner, groupId, member);
        createDebt(owner, groupId, member.id(), owner.id());

        JsonNode result = graphql(owner.token(), """
                mutation {
                  deleteGroup(id: "%s")
                }
                """.formatted(groupId));

        assertFalse(result.has("errors"));
        assertTrue(result.get("data").get("deleteGroup").asBoolean());
    }
}