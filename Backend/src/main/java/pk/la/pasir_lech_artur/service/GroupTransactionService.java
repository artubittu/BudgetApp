package pk.la.pasir_lech_artur.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pk.la.pasir_lech_artur.dto.GroupTransactionDTO;
import pk.la.pasir_lech_artur.model.Debt;
import pk.la.pasir_lech_artur.model.Group;
import pk.la.pasir_lech_artur.model.Membership;
import pk.la.pasir_lech_artur.model.Transaction;
import pk.la.pasir_lech_artur.model.TransactionType;
import pk.la.pasir_lech_artur.model.User;
import pk.la.pasir_lech_artur.repository.DebtRepository;
import pk.la.pasir_lech_artur.repository.GroupRepository;
import pk.la.pasir_lech_artur.repository.MembershipRepository;
import pk.la.pasir_lech_artur.repository.TransactionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupTransactionService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final TransactionRepository transactionRepository;
    private final MembershipService membershipService;
    private final GroupNotificationService groupNotificationService;

    public GroupTransactionService(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            DebtRepository debtRepository,
            TransactionRepository transactionRepository,
            MembershipService membershipService,
            GroupNotificationService groupNotificationService) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.debtRepository = debtRepository;
        this.transactionRepository = transactionRepository;
        this.membershipService = membershipService;
        this.groupNotificationService = groupNotificationService;
    }

    @Transactional
    public void addGroupTransaction(GroupTransactionDTO transactionDTO, User currentUser) {
        Group group = groupRepository.findById(transactionDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono grupy"));

        membershipService.assertCurrentUserIsGroupMember(group.getId());

        List<Membership> members = membershipRepository.findByGroupId(group.getId());
        List<Membership> selectedMembers = selectParticipants(transactionDTO, members);

        if (selectedMembers.isEmpty()) {
            throw new IllegalStateException("Nie wybrano uczestników transakcji grupowej.");
        }

        TransactionType transactionType = TransactionType.valueOf(transactionDTO.getType());
        double totalAmount = transactionDTO.getAmount();
        double amountPerUser = totalAmount / selectedMembers.size();

        if (transactionType == TransactionType.EXPENSE) {
            handleGroupExpense(transactionDTO, group, selectedMembers, currentUser, totalAmount, amountPerUser);
        } else {
            handleGroupIncome(transactionDTO, selectedMembers, totalAmount, amountPerUser, currentUser);
        }
    }

    private void handleGroupExpense(
            GroupTransactionDTO transactionDTO,
            Group group,
            List<Membership> selectedMembers,
            User currentUser,
            double totalAmount,
            double amountPerUser) {

        saveTransactionForUser(
                currentUser,
                totalAmount,
                TransactionType.EXPENSE,
                "Transakcja grupowa",
                transactionDTO.getTitle()
        );

        for (Membership member : selectedMembers) {
            User participant = member.getUser();

            if (participant.getId().equals(currentUser.getId())) {
                continue;
            }

            saveTransactionForUser(
                    participant,
                    amountPerUser,
                    TransactionType.EXPENSE,
                    "Transakcja grupowa",
                    transactionDTO.getTitle()
            );

            Debt debt = new Debt();
            debt.setDebtor(participant);
            debt.setCreditor(currentUser);
            debt.setGroup(group);
            debt.setAmount(amountPerUser);
            debt.setTitle(transactionDTO.getTitle());
            debtRepository.save(debt);

            groupNotificationService.notifyGroupExpenseAdded(
                    group,
                    participant,
                    currentUser,
                    transactionDTO.getTitle(),
                    totalAmount,
                    amountPerUser
            );
        }
    }

    private void handleGroupIncome(
            GroupTransactionDTO transactionDTO,
            List<Membership> selectedMembers,
            double totalAmount,
            double amountPerUser,
            User currentUser) {

        for (Membership member : selectedMembers) {
            User participant = member.getUser();

            double amountForUser = participant.getId().equals(currentUser.getId())
                    ? totalAmount
                    : amountPerUser;

            saveTransactionForUser(
                    participant,
                    amountForUser,
                    TransactionType.INCOME,
                    "Transakcja grupowa",
                    transactionDTO.getTitle()
            );
        }
    }

    private void saveTransactionForUser(
            User user,
            double amount,
            TransactionType transactionType,
            String tags,
            String notes) {

        Transaction transaction = new Transaction(
                amount,
                transactionType,
                tags,
                notes,
                user
        );

        transaction.setTimestamp(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    private List<Membership> selectParticipants(
            GroupTransactionDTO transactionDTO,
            List<Membership> members) {

        List<Long> selectedUserIds = transactionDTO.getSelectedUserIds();

        if (selectedUserIds == null || selectedUserIds.isEmpty()) {
            return members;
        }

        Set<Long> uniqueSelectedUserIds = selectedUserIds.stream()
                .collect(Collectors.toSet());

        List<Membership> selectedMembers = members.stream()
                .filter(membership -> uniqueSelectedUserIds.contains(membership.getUser().getId()))
                .toList();

        if (selectedMembers.size() != uniqueSelectedUserIds.size()) {
            throw new IllegalStateException("Wszyscy wybrani użytkownicy muszą być członkami grupy.");
        }

        return selectedMembers;
    }
}