package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.entities.NotificationTask;
import pro.sky.telegrambot.repositories.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final NotificationTaskRepository notificationTaskRepository;
    private final Pattern pattern = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+]+)");

    @Autowired
    private TelegramBot telegramBot;

    public TelegramBotUpdatesListener(NotificationTaskRepository notificationTaskRepository) {
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            if (update.message().text().equals("/start")) {
                telegramBot.execute(new SendMessage(update.message().chat().id(),
                        "Запланируйте задачу в формате: \"дд.мм.гггг чч:мм Задача\""));
            }
            Matcher matcher = pattern.matcher(update.message().text());
            if (matcher.matches()) {
                LocalDateTime date = LocalDateTime.parse(matcher.group(1),
                        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                String text = matcher.group(3);
                notificationTaskRepository.save(new NotificationTask(update.message().chat().id(), text, date));
                logger.info("NotificationTask was saved successfully");
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void run() {
        logger.info("Processing Scheduled in time: {}", LocalDateTime.now());
        List<NotificationTask> notificationTasks = notificationTaskRepository
                .findAllByDate(LocalDateTime.now()
                        .truncatedTo(ChronoUnit.MINUTES));
        if (!notificationTasks.isEmpty()) {
            notificationTasks.forEach(notificationTask -> {

                telegramBot.execute(new SendMessage(
                        notificationTask.getChatId(),
                        notificationTask.getMessage()));
                logger.info("The message: {} has been sent successfully", notificationTask.getMessage());
                notificationTaskRepository
                        .deleteById(notificationTask.getId());
                logger.info("NotificationTask with id: {} has been deleted successfully", notificationTask.getId());
            });
        } else {
            logger.info("NotificationTask for the time: {} is missing", LocalDateTime.now());
        }
    }
}
