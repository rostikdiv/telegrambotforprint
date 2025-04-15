package com.example.printbot.telegram;

import com.example.printbot.service.FileService;
import com.example.printbot.service.PdfService;
import com.example.printbot.model.Order;
import com.example.printbot.model.User;
import com.example.printbot.service.OrderCalculationService;
import com.example.printbot.service.OrderService;
import com.example.printbot.service.UserService;
import com.example.printbot.util.MessageTemplates;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.stream.Collectors;
@Component

public class BotHandler {

    public enum OrderState {
        WAITING_FOR_DESCRIPTION,
        WAITING_FOR_PAGES,
        WAITING_FOR_PRINT_TYPE,
        WAITING_FOR_COLOR,
        WAITING_FOR_PAPER,
        WAITING_FOR_FILE,
        CONFIRMATION,
        WAITING_FOR_CANCEL_COMMENT
    }

    private final UserService userService;
    private final OrderService orderService;
    private final OrderCalculationService orderCalculationService;
    private final PdfService pdfService;
    private final FileService fileService;

    private static final Logger logger = LoggerFactory.getLogger(BotHandler.class);

    private OrderState orderState;

    private final Long executorChatId = 123456789L;
    private Order order;


    @Autowired
    private PrintBot printBot;
    private void sendMessage(Long chatId, String text, ReplyKeyboard keyboard) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(keyboard)
                .build();
        printBot.sendMessage(message);
    }
    public BotHandler(UserService userService, OrderService orderService, OrderCalculationService orderCalculationService) {
        this.userService = userService;
        this.orderService = orderService;
        this.orderCalculationService = orderCalculationService;
        this.pdfService = pdfService;
    }


    /**
     * Creates a keyboard with Confirm and Cancel buttons for order confirmation.
     *
     * @return An InlineKeyboardMarkup with Confirm and Cancel buttons.
     */
    private InlineKeyboardMarkup createConfirmationKeyboard() {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        confirmButton.setText("Confirm");
        confirmButton.setCallbackData("/confirm_order");
        rowInline.add(confirmButton);

        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("Cancel");
        cancelButton.setCallbackData("/cancel_order");
        rowInline.add(cancelButton);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    /**
     * Creates a keyboard with buttons to update the order status.
     *
     * @param orderId The ID of the order to update.
     * @return An InlineKeyboardMarkup with status update buttons.
     */
    private InlineKeyboardMarkup createUpdateStatusKeyboard(Long orderId, String messageId) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton> rowInline = new ArrayList<>();

        org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton canceledButton = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
        canceledButton.setText("CANCELED");
        canceledButton.setCallbackData("/update_status " + orderId + " CANCELED " + messageId);
        rowInline.add(canceledButton);

        org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton acceptedButton = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
        acceptedButton.setText("ACCEPTED");
        acceptedButton.setCallbackData("/update_status " + orderId + " ACCEPTED " + messageId);
        rowInline.add(acceptedButton);

        org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton paidButton = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
        paidButton.setText("PAID");
        paidButton.setCallbackData("/update_status " + orderId + " PAID " + messageId);
        rowInline.add(paidButton);

        org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton completedButton = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
        completedButton.setText("COMPLETED");
        completedButton.setCallbackData("/update_status " + orderId + " COMPLETED " + messageId);
        rowInline.add(completedButton);


        rowsInline.add(rowInline);

        markupInline.setKeyboard(rowsInline);

        return markupInline;
    }

    /**
     * Handles incoming updates from Telegram.
     *
     * @param update The update object from Telegram.
     * @return A SendMessage object to reply to the user.
     */
    public SendMessage handleUpdate(Update update) {
        // Check if update contains message
        if (update.hasMessage()) {
            Message message = update.getMessage();
            //Check if message is text message
            if (message.hasText()) {
                return handleTextMessage(update);
            } else if (message.hasDocument()) {
                return handleDocument(update);
            }
            else {
                return sendValidationErrorMessage(update.getMessage().getChatId(), MessageTemplates.UNKNOWN_ERROR.getMessage());
            }
        } else if (update.hasCallbackQuery()) {
            // Check if update contains callback query
            return handleCallbackQuery(update);
        } else if (orderState == OrderState.WAITING_FOR_CANCEL_COMMENT) {
            return handleCancelComment(update);

        }
        logger.warn("Received update without message or callback query");
        return null;
    }
    /**
     * Handles the document from the user.
     *
     * @param update The update object from Telegram.
     * @return A SendMessage object to reply to the user.
     */
    private SendMessage handleDocument(Update update) {
        SendMessage message = new SendMessage();

        Document document = update.getMessage().getDocument();
        if (!document.getMimeType().equals("application/pdf")) {
            logger.warn("User {} try to upload file with invalid type {}", update.getMessage().getFrom().getId(), document.getMimeType());
            return sendValidationErrorMessage(update.getMessage().getChatId(),"Invalid file type. Please upload PDF file.");
        } else {
            if (document.getFileSize() > 20 * 1024 * 1024) {

                logger.warn("User {} try to upload file with size {}", update.getMessage().getFrom().getId(), document.getFileSize());
                message.setText(MessageTemplates.FILE_SIZE_ERROR.getMessage());
                return message;
            }

            order.setFileId(document.getFileId());
            if (order.getDescription() == null || order.getDescription().isEmpty()) {
                order.setDescription("Printing of " + document.getFileName());
            }
            logger.info("User {} uploaded file with id {}", update.getMessage().getFrom().getId(), document.getFileId());
            if (orderState == OrderState.WAITING_FOR_FILE) {
                return handleTextMessage(update);
            }
        }

        return message;
    }
    private SendMessage sendValidationErrorMessage(Long chatId, String text) {
        logger.warn("Validation error. Chat ID: {}, Message: {}", chatId, text);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();
    }
    private org.telegram.telegrambots.meta.api.objects.File getFileFromTelegram(String fileId) {
        try {
            return printBot.execute(new org.telegram.telegrambots.meta.api.methods.GetFile(fileId));
        } catch (Exception e) {
            logger.error("Error getting file from Telegram", e);
            return null;
        }
    }

    /**
     * Handles text messages from the user.
     *
     * @param update The update object from Telegram.
     * @return A SendMessage object to reply to the user.
     */
    private SendMessage handleTextMessage(Update update) {
        Long telegramId = update.getMessage().getFrom().getId();
        User user = userService.findUserByTelegramId(telegramId);
        if (user == null) {
            user = new User();
            user.setTelegramId(telegramId);
            user.setContactInfo("");
            user.setUsername(update.getMessage().getFrom().getUserName());
            userService.save(user);
        }
        String messageText = update.getMessage().getText();

        // Handle /start command
        if (messageText.equals("/start")) {
            sendMessage(update.getMessage().getChatId(), MessageTemplates.GREETING.getMessage(), null);
            // Handle /create_order command
        } else if (messageText.equals("/create_order")) { //If message equals /create_order
            return handleCreateOrderCommand(update);

            // Handle state of order creation
        } else if (orderState != null) { //If orderState is not null
            return handleOrderState(update);
        // Handle /my_orders command
        } else if (messageText.equals("/my_orders")) { //If message equals /my_orders
            return handleMyOrdersCommand(update);
            // Handle any other text message (echo)
        } else {
            sendMessage(update.getMessage().getChatId(), messageText, null);//if no command or state - just echo message
        }
        return null;
    }
    /**
     * Handles the /create_order command.
     *
     * @param update The update object from Telegram.
     * @return A SendMessage object to reply to the user.
     */
    private SendMessage handleCreateOrderCommand(Update update) {
        try {
            order = new Order();
            order.setUserId(update.getMessage().getFrom().getId());
            order.setStatus(Order.Status.ACCEPTED.name());
            order.setOrderNumber(orderService.generateOrderNumber());
            sendMessage(update.getMessage().getChatId(), MessageTemplates.ORDER_DESCRIPTION_REQUEST.getMessage(), null);

            orderState = OrderState.WAITING_FOR_DESCRIPTION;
            logger.info("User {} start creating order with number {}", update.getMessage().getFrom().getId(), order.getOrderNumber());
        } catch (Exception e) {
            logger.error("Error in handleCreateOrderCommand", e);
            sendValidationErrorMessage(update.getMessage().getChatId(),"An error occurred while creating the order. Please try again.");
        }
        return null;
    }


    
    /**
     * Handles the state of order creation.
     *
     * @param update The update object from Telegram.
     * @return A SendMessage object to reply to the user.
     */
    private SendMessage handleOrderState(Update update) {
            try {
                String messageText = update.getMessage().getText();
                Long chatId = update.getMessage().getChatId();
                if (messageText.isEmpty()) { // Check if input is empty
                    logger.warn("User {} send empty input", update.getMessage().getFrom().getId()); // Log empty input
                    return sendValidationErrorMessage(chatId, "Input cannot be empty. Please try again.");

                }

                if (orderState == OrderState.WAITING_FOR_DESCRIPTION) { //If state is WAITING_FOR_DESCRIPTION
                    order.setDescription(messageText); // Set order description
                    orderState = OrderState.WAITING_FOR_PAGES; // Set next state
                    sendMessage(chatId, MessageTemplates.ORDER_PAGES_REQUEST.getMessage(), null); // Send message to user
                } else if (orderState == OrderState.WAITING_FOR_PAGES) { //If state is WAITING_FOR_PAGES
                    try {
                        //Check if input is number
                        int pages = Integer.parseInt(messageText); // Parse input to integer
                        if (pages <= 0) { // Check if pages less or equals 0
                            logger.warn("User {} enter invalid number of pages: {}", update.getMessage().getFrom().getId(), pages); // Log invalid number of pages
                            return sendValidationErrorMessage(chatId,MessageTemplates.VALIDATION_ERROR.getMessage() + " Number of pages must be greater than 0. Please try again.");

                        }
                        order.setPages(pages); // Set order pages
                    } catch (NumberFormatException e) { // Catch exception if input is not a number
                        logger.warn("User {} enter invalid number format of pages: {}", update.getMessage().getFrom().getId(), messageText); // Log invalid number format of pages
                        return sendValidationErrorMessage(chatId, MessageTemplates.VALIDATION_ERROR.getMessage() + " Invalid input. Please enter a valid number for pages.");
                    }
                    orderState = OrderState.WAITING_FOR_PRINT_TYPE; // Set next state
                    sendMessage(chatId, MessageTemplates.ORDER_PRINT_TYPE_REQUEST.getMessage(), null); // Send message to user
                } else if (orderState == OrderState.WAITING_FOR_PRINT_TYPE) { //If state is WAITING_FOR_PRINT_TYPE
                    order.setPrintType(messageText); // Set order print type
                    orderState = OrderState.WAITING_FOR_COLOR; // Set next state
                    sendMessage(chatId, MessageTemplates.ORDER_COLOR_REQUEST.getMessage(), null); // Send message to user

                } else if (orderState == OrderState.WAITING_FOR_COLOR) { //If state is WAITING_FOR_COLOR
                    order.setColor(messageText); // Set order color
                    orderState = OrderState.WAITING_FOR_PAPER; // Set next state
                    sendMessage(chatId, MessageTemplates.ORDER_PAPER_REQUEST.getMessage(), null); // Send message to user
                } else if (orderState == OrderState.WAITING_FOR_PAPER) { //If state is WAITING_FOR_PAPER
                    order.setPaper(messageText); // Set order paper
                    orderState = OrderState.WAITING_FOR_FILE; // Set next state
                    sendMessage(chatId, MessageTemplates.ORDER_FILE_REQUEST.getMessage(), null); // Send message to user
                } else if (orderState == OrderState.WAITING_FOR_FILE) {

                    sendMessage(chatId, MessageTemplates.ORDER_CONFIRMATION.getMessage() + "\n" + getOrderDetails() + "\n", createConfirmationKeyboard());



                    orderState = OrderState.CONFIRMATION;
                } else if (orderState == OrderState.WAITING_FOR_CANCEL_COMMENT){
                    return handleCancelComment(update);
                }

                return message;
            }
            catch (Exception e) {
                logger.error("Error in handleOrderState", e); // Log the error
                return sendValidationErrorMessage(update.getMessage().getChatId(), MessageTemplates.UNKNOWN_ERROR.getMessage());
            }

    }

    private String getOrderDetails(){
        return  "Description: " + order.getDescription() + "\n" +
                "Pages: " + order.getPages() + "\n" +
                "Print Type: " + order.getPrintType() + "\n" +
                "Color: " + order.getColor() + "\n" +
                "Paper: " + order.getPaper();
    }


    /**
     * Handles the /my_orders command.
     *
     * @param user    The user initiating the command.
     * @param message The message object to reply to the user.
     * @return A SendMessage object to reply to the user.
     */
    private SendMessage handleMyOrdersCommand(Update update) {
        SendMessage message = new SendMessage();
        try {
            List<Order> orders = orderService.findOrdersByUserId(update.getMessage().getFrom().getId());
            String ordersList = orders.stream() // Stream the list of orders
                    .map(order -> String.format(
                                    "Order #%s\n" +
                                            "Status: %s\n" + // Order status
                                            "Description: %s\n" + // Order description
                                            "Pages: %s\n" + // Order pages
                                            "Cost: %s\n" + // Order cost
                                            "FileId: %s\n" + // Order file id
                                            "CancelComment: %s", // Order cancel comment
                            // Format the order details
                            order.getOrderNumber(), order.getStatus(), order.getDescription(), order.getPages(), order.getCost(), order.getFileId() == null ? "Not provided" : order.getFileId(), order.getCancelComment() == null ? "Not provided" : order.getCancelComment()))
                    .collect(Collectors.joining("\n"));
            logger.info("User {} get list of orders", update.getMessage().getFrom().getId());
            message.setChatId(update.getMessage().getChatId());
            message.setText(ordersList.isEmpty() ? "No orders found." : ordersList);
            return message;
        } catch (Exception e) {
            logger.error("Error in handleMyOrdersCommand", e);
            message.setText("An error occurred while retrieving your orders. Please try again.");
            return message;
        }


    }
    /**
     * Handles callback queries from inline keyboards.
     *
     * @param update The update object from Telegram.
     * @return A SendMessage object to reply to the user.
     */
    private SendMessage handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callbackData = callbackQuery.getData();
        logger.info("Get callback query: {}", callbackData);
        SendMessage message = new SendMessage();
        // Set chat id for message
        message.setChatId(callbackQuery.getMessage().getChatId().toString());
        // Check if callback data contains /update_status command
        if (callbackData.startsWith("/update_status")) {
            return handleUpdateStatusCommand(callbackQuery, message);
        } else if (callbackData.equals("/confirm_order")) {
            return handleConfirmOrderCommand(callbackQuery, message);
        } else if (callbackData.equals("/cancel_order")) {
            return handleCancelOrderCommand(callbackQuery, message);//If callback data equals /cancel_order
        }
        return null;
    }

    /**
     * Handles the /cancel_order command.
     *
     * @param callbackQuery The callback query object from Telegram.
     * @param message       The message object to reply to the user.
     * @return A SendMessage object to reply to the user.
     */
    private SendMessage handleCancelOrderCommand(CallbackQuery callbackQuery, SendMessage message) {
        try {
            sendMessage(callbackQuery.getMessage().getChatId(), MessageTemplates.ORDER_CANCELED_COMMENT_REQUEST.getMessage(), null); // Send message to user
            order.setStatus(Order.Status.CANCELED.name()); // Set order status to CANCELED
            orderState = OrderState.WAITING_FOR_CANCEL_COMMENT; // Set state to WAITING_FOR_CANCEL_COMMENT
            logger.info("Order {} canceled", order.getOrderNumber());
        } catch (Exception e) {
            logger.error("Error in handleCancelOrderCommand", e);
            return sendValidationErrorMessage(callbackQuery.getMessage().getChatId(), "An error occurred while canceling the order. Please try again.");
        }
        return null;
    }

    /**
     * Handles the /confirm_order command.
     *
     * @param callbackQuery The callback query object from Telegram.
     * @param message       The message object to reply to the user.
     * @return A SendMessage object to reply to the user.
     */
    private SendMessage handleConfirmOrderCommand(CallbackQuery callbackQuery, SendMessage message) {
        try {
            double cost = orderCalculationService.calculateCost(order);
            order.setCost(cost);
            orderService.save(order);
            orderState = null;
            sendExecutorNotification(order);
            logger.info("Order {} created successfully with cost: {}", order.getOrderNumber(), order.getCost()); // Log order creation
            sendMessage(callbackQuery.getMessage().getChatId(), MessageTemplates.ORDER_CREATED.getMessage() + ": " + order.getCost(), null);

            return message; // Return message

        catch (Exception e) {
            logger.error("Error in handleConfirmOrderCommand", e);
            message.setText("An error occurred while confirming the order. Please try again.");
            return message;

        }
    }

    /**
     * Sends a notification to the executor about the new order.
     *
     * @param order The order to send notification about.
     */
    private void sendExecutorNotification(Order order){
        sendMessage(executorChatId, MessageTemplates.EXECUTOR_NEW_ORDER.getMessage() + order.getOrderNumber() + "\n" +
                getOrderDetails() + "\n" +
                "File Id: " + (order.getFileId() == null ? "Not provided" : order.getFileId()) + "\n", null);
    }

    private void sendValidationErrorMessage(Long chatId, String errorMessage) {
        sendMessage(chatId, errorMessage, null);

    }
    private SendMessage handleUpdateStatusCommand(CallbackQuery callbackQuery, SendMessage message) {

        try {
            String callbackData = callbackQuery.getData();
            String[] parts = callbackData.split(" ");
            Long orderId = null;
            // Get order id from callback data
            orderId = Long.parseLong(parts[1]);
            String status = parts[2];
            //Check if status is valid
            Order.Status.valueOf(status);
            orderService.updateOrderStatus(orderId, Order.Status.valueOf(status));
            Order order = orderService.findOrderById(orderId);
            message.setText("Order status updated to: " + order.getStatus());
            message.setChatId(callbackQuery.getMessage().getChatId().toString());
            message.setMessageThreadId(callbackQuery.getMessage().getMessageThreadId());
            //Create keyboard with buttons to update status
            message.setReplyMarkup(createUpdateStatusKeyboard(orderId, parts[3]));
            return message;
        } catch (NumberFormatException e) {
            message.setText("Invalid order ID format.");
            return message;
        } catch (IllegalArgumentException e) {
            message.setText("Invalid order status.");
            return message;
        } catch (Exception e) {
            logger.error("Error in handleUpdateStatusCommand", e);
            message.setText("An error occurred while updating the order status. Please try again.");
        }

        return message;
    }

    /**
     * Handles the cancellation comment.
     *
     * @param update The update object from Telegram.
     * @return A SendMessage object to reply to the user.
     */
    private SendMessage handleCancelComment(Update update) {
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId().toString());
        try {
            String comment = update.getMessage().getText();
            if (comment.isEmpty()){
                message.setText("Comment can't be empty");
                return message;
            }
            order.setCancelComment(comment);
            orderService.save(order);
            message.setText(MessageTemplates.ORDER_CANCELED.getMessage() + ": " + comment);
            orderState = null;
            return message;
        } catch (Exception e){
            logger.error("Error in handleCancelComment", e);
            message.setText("Error while saving comment");
            return message;
        }
    }
}
