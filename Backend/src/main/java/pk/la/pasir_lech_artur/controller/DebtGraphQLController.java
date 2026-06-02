package pk.la.pasir_lech_artur.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.la.pasir_lech_artur.dto.DebtDTO;
import pk.la.pasir_lech_artur.model.Debt;
import pk.la.pasir_lech_artur.model.User;
import pk.la.pasir_lech_artur.service.CurrentUserService;
import pk.la.pasir_lech_artur.service.DebtService;

import java.util.List;

@Controller
public class DebtGraphQLController {

    private final DebtService debtService;
    private final CurrentUserService currentUserService;

    public DebtGraphQLController(DebtService debtService, CurrentUserService currentUserService){
        this.debtService = debtService;
        this.currentUserService = currentUserService;
    }

    @QueryMapping
    public List<Debt> groupDebts(@Argument Long groupId){
        return debtService.getGroupDebts(groupId);
    }

    @MutationMapping
    public Debt createDebt(@Valid @Argument DebtDTO debtDTO){
        return debtService.createDebt(debtDTO);
    }

    @MutationMapping
    public Boolean deleteDebt(@Argument Long debtId) {
        User currentUser = currentUserService.getCurrentUser();
        debtService.deleteDebt(debtId, currentUser);
        return true;
    }

    @MutationMapping
    public Debt markDebtAsPaid(@Argument Long debtId){
        return debtService.markDebtAsPaid(debtId);
    }
    @MutationMapping
    public Debt confirmDebtPayment(@Argument Long debtId){
        return debtService.confirmDebtPayment(debtId);
    }
}
