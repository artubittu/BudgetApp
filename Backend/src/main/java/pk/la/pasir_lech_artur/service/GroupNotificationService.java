package pk.la.pasir_lech_artur.service;

import org.springframework.stereotype.Service;
import pk.la.pasir_lech_artur.dto.GroupExpenseNotificationDTO;
import pk.la.pasir_lech_artur.model.Group;
import pk.la.pasir_lech_artur.model.User;
import pk.la.pasir_lech_artur.websocket.GroupNotificationWebSocketHandler;

@Service
public class GroupNotificationService {

    private final GroupNotificationWebSocketHandler groupNotificationWebSocketHandler;

    public GroupNotificationService(GroupNotificationWebSocketHandler groupNotificationWebSocketHandler) {
        this.groupNotificationWebSocketHandler = groupNotificationWebSocketHandler;
    }

    public void notifyGroupExpenseAdded(
            Group group,
            User targetUser,
            User createdBy,
            String title,
            Double amount,
            Double userShare) {

        String message = String.format(
                "%s dodał wydatek \"%s\" w grupie %s. Twoja część: %.2f zł.",
                createdBy.getEmail(),
                title,
                group.getName(),
                userShare
        );

        GroupExpenseNotificationDTO notification = new GroupExpenseNotificationDTO(
                "GROUP_EXPENSE_ADDED",
                group.getId(),
                group.getName(),
                title,
                amount,
                userShare,
                createdBy.getEmail(),
                message
        );

        groupNotificationWebSocketHandler.sendNotificationToUser(targetUser.getId(), notification);
    }
}