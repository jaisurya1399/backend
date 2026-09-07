package com.projectmanagement.app.notification;

import com.projectmanagement.app.user.User;

/** Published after an in-app ticket notification has been persisted. */
public record TicketNotificationCreatedEvent(
                User recipient,
                String type,
                Long ticketId,
                String ticketCode,
                String message) {
}
