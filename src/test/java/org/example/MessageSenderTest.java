package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.school.analysis.presentation.telegram.bot.SchoolTelegramBot;
import org.school.analysis.presentation.telegram.util.MessageSender;
import org.slf4j.Logger;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageSenderTest {

    private MessageSender messageSender;
    private SchoolTelegramBot bot;
    private Logger logger;

    @BeforeEach
    void setUp() throws Exception {
        bot = Mockito.mock(SchoolTelegramBot.class);
        messageSender = new MessageSender(bot);
        Field loggerField = MessageSender.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        logger = (Logger) loggerField.get(null);
    }

    @Test
    void sendText_sendsCorrectMessage() throws TelegramApiException {
        messageSender.sendText(123456789L, "Test Message");

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage message = messageCaptor.getValue();
        assertEquals("123456789", message.getChatId());
        assertEquals("Test Message", message.getText());
        assertEquals("HTML", message.getParseMode());
        assertNull(message.getReplyMarkup());
    }

    @Test
    void sendMenu_sendsCorrectMessageWithKeyboard() throws TelegramApiException {
        ReplyKeyboard keyboard = Mockito.mock(ReplyKeyboard.class);
        messageSender.sendMenu(123456789L, "Test Message", keyboard);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage message = messageCaptor.getValue();
        assertEquals("123456789", message.getChatId());
        assertEquals("Test Message", message.getText());
        assertEquals("HTML", message.getParseMode());
        assertEquals(keyboard, message.getReplyMarkup());
    }

    @Test
    void sendInlineMenu_sendsCorrectMessageWithInlineKeyboard() throws TelegramApiException {
        org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard = Mockito.mock(org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup.class);
        messageSender.sendInlineMenu(123456789L, "Test Message", keyboard);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage message = messageCaptor.getValue();
        assertEquals("123456789", message.getChatId());
        assertEquals("Test Message", message.getText());
        assertEquals("HTML", message.getParseMode());
        assertEquals(keyboard, message.getReplyMarkup());
    }

    @Test
    void sendPhoto_telegramApiException_sendsFallbackMessage() throws TelegramApiException {
        byte[] photoBytes = {1, 2, 3};
        String caption = "Test Caption";
        Long chatId = 123456789L;

        doThrow(new TelegramApiException("Test Exception")).when(bot).execute(any(SendPhoto.class));

        messageSender.sendPhoto(chatId, photoBytes, caption);
        ArgumentCaptor<SendPhoto> photoCaptor = ArgumentCaptor.forClass(SendPhoto.class);
        verify(bot).execute(photoCaptor.capture());

        SendPhoto sendPhoto = photoCaptor.getValue();
        assertEquals("123456789", sendPhoto.getChatId());
        assertEquals(caption, sendPhoto.getCaption());
        assertEquals("HTML", sendPhoto.getParseMode());
        ArgumentCaptor<SendMessage> textCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(textCaptor.capture());

        SendMessage sendMessage = textCaptor.getValue();
        assertEquals(chatId.toString(), sendMessage.getChatId());
        assertEquals(caption + "\nНе удалось отправить изображение", sendMessage.getText());

    }

    @Test
    void sendMessage_replyKeyboard() throws TelegramApiException {
        ReplyKeyboard keyboard = Mockito.mock(ReplyKeyboard.class);
        messageSender.sendMenu(123456789L, "Test Message", keyboard);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage message = messageCaptor.getValue();
        assertEquals("123456789", message.getChatId());
        assertEquals("Test Message", message.getText());
        assertEquals("HTML", message.getParseMode());
        assertEquals(keyboard, message.getReplyMarkup());
    }

    @Test
    void sendMessage_inlineKeyboard() throws TelegramApiException {
        org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard = Mockito.mock(org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup.class);
        messageSender.sendInlineMenu(123456789L, "Test Message", keyboard);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage message = messageCaptor.getValue();
        assertEquals("123456789", message.getChatId());
        assertEquals("Test Message", message.getText());
        assertEquals("HTML", message.getParseMode());
        assertEquals(keyboard, message.getReplyMarkup());
    }

    @Test
    void sendText_htmlParsingEnabledByDefault() throws TelegramApiException {
        messageSender.sendText(123456789L, "Test Message");

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage message = messageCaptor.getValue();
        assertEquals("HTML", message.getParseMode());
    }

    @Test
    void sendPhoto_emptyCaption_sendsPhoto() throws TelegramApiException {
        byte[] photoBytes = {1, 2, 3};
        String caption = ""; // Empty caption
        messageSender.sendPhoto(123456789L, photoBytes, caption);

        ArgumentCaptor<SendPhoto> photoCaptor = ArgumentCaptor.forClass(SendPhoto.class);
        verify(bot).execute(photoCaptor.capture());

        SendPhoto sendPhoto = photoCaptor.getValue();
        assertEquals("", sendPhoto.getCaption());
    }

    @Test
    void sendPhoto_nullCaption_sendsPhoto() throws TelegramApiException {
        byte[] photoBytes = {1, 2, 3};
        String caption = null; // Null caption
        messageSender.sendPhoto(123456789L, photoBytes, caption);

        ArgumentCaptor<SendPhoto> photoCaptor = ArgumentCaptor.forClass(SendPhoto.class);
        verify(bot).execute(photoCaptor.capture());

        SendPhoto sendPhoto = photoCaptor.getValue();
        assertNull(sendPhoto.getCaption());
    }
}