package org.school.analysis.infrastructure.telegram.util;

import org.school.analysis.infrastructure.telegram.bot.SchoolTelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;

public class MessageSender {
    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);
    private final SchoolTelegramBot bot;

    public MessageSender(SchoolTelegramBot bot) {
        this.bot = bot;
    }

    public void sendText(Long chatId, String text) {
        sendMessage(chatId, text, null, true);
    }

    public void sendMenu(Long chatId, String text, ReplyKeyboard keyboard) {
        sendMessage(chatId, text, keyboard, true);
    }

    public void sendInlineMenu(Long chatId, String text,
                               org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard) {
        sendMessage(chatId, text, keyboard, true);
    }


    public void sendPhoto(Long chatId, byte[] photoBytes, String caption) {
        try {
            InputFile photo = new InputFile();
            photo.setMedia(new ByteArrayInputStream(photoBytes), "chart.png");

            SendPhoto sendPhoto = SendPhoto.builder().chatId(chatId.toString()).photo(photo).caption(caption).parseMode("HTML")
                    .build();

            bot.execute(sendPhoto);

        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки изображения {}", chatId, e);
            sendText(chatId, caption + "\nНе удалось отправить изображение");
        }
    }

    private void sendMessage(Long chatId, String text, Object keyboard, boolean parseHtml) {
        try {
            SendMessage.SendMessageBuilder builder = SendMessage.builder().chatId(chatId.toString()).text(text);
            if (parseHtml) {
                builder.parseMode("HTML");
            }
            if (keyboard != null) {
                if (keyboard instanceof ReplyKeyboard) {
                    builder.replyMarkup((ReplyKeyboard) keyboard);
                } else if (keyboard instanceof org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup) {
                    builder.replyMarkup((org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup) keyboard);
                }
            }

            bot.execute(builder.build());
        } catch (TelegramApiException e) {
            logger.error("шибка отправки изображения {}", chatId, e);
        }
    }

}