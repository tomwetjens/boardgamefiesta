package com.boardgamefiesta.server.ses;

import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import lombok.NonNull;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Message;

import javax.enterprise.context.ApplicationScoped;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@ApplicationScoped
public class EmailTemplates {

    private final Translations translations;
    private final String url;

    private final VelocityEngine velocityEngine;

    public EmailTemplates(@NonNull Translations translations,
                          @ConfigProperty(name = "bgf.url") String url) {
        this.translations = translations;
        this.url = url;

        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty("classpath." + RuntimeConstants.RESOURCE_LOADER + "." + RuntimeConstants.RESOURCE_LOADER_CLASS, ClasspathResourceLoader.class.getName());
        velocityEngine.init();
    }

    Message createBeginTurnMessage(Table.BeginTurn event, User user) {
        var context = createDefaultContext(user.getLocale(), user.getTimeZone());
        context.put("event", event);
        context.put("user", user);

        var template = velocityEngine.getTemplate("/templates/begin_turn.vm");
        var writer = new StringWriter();
        template.merge(context, writer);

        return Message.builder()
                .subject(Content.builder()
                        .charset(StandardCharsets.UTF_8.name())
                        .data(extractTitle(writer.toString()))
                        .build())
                .body(Body.builder()
                        .html(Content.builder()
                                .charset(StandardCharsets.UTF_8.name())
                                .data(writer.toString())
                                .build())
                        .build())
                .build();
    }

    private VelocityContext createDefaultContext(Locale locale, ZoneId timeZone) {
        var context = new VelocityContext();
        context.put("url", url);
        context.put("dateTime", new DateTimeTool(locale, timeZone));
        context.put("translations", new TranslationsTool(translations, locale));
        return context;
    }

    private String extractTitle(String html) {
        var matcher = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        throw new IllegalStateException("No title tag found in HTML: " + html);
    }

    Message createInvitedMessage(Table.Invited event, User user, User host) {
        var context = createDefaultContext(user.getLocale(), user.getTimeZone());
        context.put("event", event);
        context.put("user", user);
        context.put("host", host);

        var template = velocityEngine.getTemplate("/templates/invited.vm");
        var writer = new StringWriter();
        template.merge(context, writer);

        return Message.builder()
                .subject(Content.builder()
                        .charset(StandardCharsets.UTF_8.name())
                        .data(extractTitle(writer.toString()))
                        .build())
                .body(Body.builder()
                        .html(Content.builder()
                                .charset(StandardCharsets.UTF_8.name())
                                .data(writer.toString())
                                .build())
                        .build())
                .build();
    }

    public Message createEndedMessage(Table table, Player player, Map<User.Id, User> userMap) {
        var user = userMap.get(player.getUserId().get());

        var context = createDefaultContext(user.getLocale(), user.getTimeZone());
        context.put("table", table);
        context.put("player", player);
        context.put("user", user);
        context.put("userMap", userMap);

        var template = velocityEngine.getTemplate("/templates/ended.vm");
        var writer = new StringWriter();
        template.merge(context, writer);

        return Message.builder()
                .subject(Content.builder()
                        .charset(StandardCharsets.UTF_8.name())
                        .data(extractTitle(writer.toString()))
                        .build())
                .body(Body.builder()
                        .html(Content.builder()
                                .charset(StandardCharsets.UTF_8.name())
                                .data(writer.toString())
                                .build())
                        .build())
                .build();
    }

}
