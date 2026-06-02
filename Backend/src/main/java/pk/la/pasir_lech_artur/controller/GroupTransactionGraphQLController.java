package pk.la.pasir_lech_artur.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import pk.la.pasir_lech_artur.dto.GroupTransactionDTO;
import pk.la.pasir_lech_artur.model.User;
import pk.la.pasir_lech_artur.service.CurrentUserService;
import pk.la.pasir_lech_artur.service.GroupTransactionService;

@Controller
public class GroupTransactionGraphQLController {

    private final GroupTransactionService groupTransactionService;
    private final CurrentUserService currentUserService;

    public GroupTransactionGraphQLController(
            GroupTransactionService groupTransactionService,
            CurrentUserService currentUserService){
        this.groupTransactionService = groupTransactionService;
        this.currentUserService = currentUserService;
    }

    @MutationMapping
    public Boolean addGroupTransaction(@Valid @Argument GroupTransactionDTO groupTransactionDTO){
        User user = currentUserService.getCurrentUser();

        groupTransactionService.addGroupTransaction(groupTransactionDTO, user);
        return true;
    }
}
