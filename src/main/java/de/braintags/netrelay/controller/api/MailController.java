/*
 * #%L
 * vertx-pojongo
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.controller.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.braintags.io.vertx.util.CounterObject;
import de.braintags.io.vertx.util.exception.InitException;
import de.braintags.netrelay.NetRelay;
import de.braintags.netrelay.controller.AbstractController;
import de.braintags.netrelay.controller.ThymeleafTemplateController;
import de.braintags.netrelay.routing.RouterDefinition;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;

/**
 * A controller which is sending mails by using the {@link NetRelay#getMailClient()}. The controller can compose the
 * content of the mail by using a static text, which will be set inside the configuration. Or - if a template is defined
 * by the configuration - the content will be generated dynamic. Most of the parameters, which are used by the
 * controller, can be set either by configuration or request parameters.
 * 
 * 
 * Config-Parameter:<br/>
 * Most parameters are settable by configuration properties, request parameters and by the context.
 * <UL>
 * <LI>{@value #FROM_PARAM}
 * <LI>{@value #TO_PARAMETER}
 * <LI>{@value #BOUNCE_ADDRESS_PARAM}
 * <LI>{@value #SUBJECT_PARAMETER}
 * <LI>{@value #TEXT_PARAMETER}
 * <LI>{@value #HTML_PARAMETER}
 * <LI>{@value #TEMPLATE_PARAM}
 * <LI>{@value #INLINE_PROP}
 * <LI>{@value #HOSTNAME_PROP}
 * <LI>{@value #PORT_PROP}
 * <LI>{@value #SCHEME_PROP}
 * <LI>{@value #STORE_RESULT_VARIABLE_PARAMETER}
 * </UL>
 * <br>
 * 
 * Request-Parameter:<br/>
 * most of the parameters, which can be set by the properties, can be set by request parameters either
 * <UL>
 * <LI>{@value #FROM_PARAM}
 * <LI>{@value #TO_PARAMETER}
 * <LI>{@value #SUBJECT_PARAMETER}
 * <LI>{@value #TEXT_PARAMETER}
 * <LI>{@value #HTML_PARAMETER}
 * <LI>{@value #TEMPLATE_PARAM}
 * </UL>
 * <br/>
 * 
 * Result-Parameter:<br/>
 * The controller sends back a json reply with the information success ( true / false ), errorMessage and
 * {@link MailSendResult}
 *
 * @author Michael Remme
 * 
 */
public class MailController extends AbstractController {
  // TODO change composer and sender to Verticle - https://github.com/BraintagsGmbH/netrelay/issues/3

  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(MailController.class);
  private static final Pattern IMG_PATTERN = Pattern.compile("(<img [^>]*src=\")([^\"]+)(\"[^>]*>)");

  /**
   * If this parameter is defined, the result of the mail sending will be stored under the variable in the context.
   * Otherwise the result will be returned as page request
   */
  public static final String STORE_RESULT_VARIABLE_PARAMETER = "storeResultVariable";

  /**
   * The name of the parameter, by which the sender of mails is defined. Ii is read from properties or request.
   */
  public static final String FROM_PARAM = "from";

  /**
   * The parameter inside the configuration properties by which the ( optional ) address for bounder is set
   */
  public static final String BOUNCE_ADDRESS_PARAM = "bounceAddress";

  /**
   * The name of the parameter inside the request or properties, by which the address to send the mail to is set
   */
  public static final String TO_PARAMETER = "to";

  /**
   * the parameter inside the request or properties, which is specifying the mail subject
   */
  public static final String SUBJECT_PARAMETER = "subject";
  /**
   * The parameter inside the properties, request or context, which contains the mail text to be sent
   */
  public static final String TEXT_PARAMETER = "mailText";

  /**
   * The parameter inside the properties, request or context, which contains the HTML text to be sent
   */
  public static final String HTML_PARAMETER = "htmlText";

  /**
   * With this parameter the template can be set, which will be parsed to generate the content of the mail. It is read
   * from properties, request or context. NOTE: if the content of the mail shall be created by a template, then the
   * configuration must define the properties, which are needed by {@link ThymeleafTemplateController}
   */
  public static final String TEMPLATE_PARAM = "template";

  /**
   * With this parameter of the properties it is defined, wether images, which are referenced inside the mail content,
   * shall be integrated inline
   */
  public static final String INLINE_PROP = "inline";

  /**
   * Property by which the hostname can be set. The hostname will be placed into the context, before processing a
   * template, so that it can be used inside link buildings, for instance
   */
  public static final String HOSTNAME_PROP = "host";

  /**
   * Property by which the port can be set. The port will be placed into the context, before processing the
   * template, so that it can be used inside link buildings, for instance
   */
  public static final String PORT_PROP = "port";

  /**
   * Property by which the scheme can be set. The scheme will be placed into the context, before processing the
   * template, so that it can be used inside link buildings, for instance
   */
  public static final String SCHEME_PROP = "scheme";

  private static final String UNCONFIGURED_ERROR = "The MailClient of NetRelay is not started, check the configuration and restart server!";

  private MailPreferences prefs;
  private static int seq;
  private static String hostname = "localhost";

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public void handle(RoutingContext context) {
    sendMail(context, getNetRelay().getMailClient(), prefs, result -> sendReply(context, result.result()));
  }

  private void sendReply(RoutingContext context, MailSendResult result) {
    String varname = readProperty(STORE_RESULT_VARIABLE_PARAMETER, null, false);
    if (varname == null) {
      HttpServerResponse response = context.response();
      response.putHeader("content-type", "application/json; charset=utf-8").end(Json.encodePrettily(result));
    } else {
      context.put(varname, Json.encodePrettily(result));
      context.next();
    }
  }

  /**
   * The method composes and sends a mail message. Note: this method won't cause a fail on the given handler, it calls
   * always the success method of the handler. If errors occured, they will be set inside the returned
   * {@link MailSendResult},
   * where then the property {@link MailSendResult#success} is set to false
   * 
   * @param context
   *          the current context
   * @param mailClient
   *          the {@link MailClient} to be used
   * @param prefs
   *          the {@link MailPreferences} created from the controller properties
   * @param handler
   *          the handler to be informed. The handler receives an instance of {@link MailSendResult}
   */
  public static void sendMail(RoutingContext context, MailClient mailClient, MailPreferences prefs,
      Handler<AsyncResult<MailSendResult>> handler) {
    try {
      if (mailClient == null) {
        throw new InitException(UNCONFIGURED_ERROR);
      }
      URI uri = URI.create(context.request().absoluteURI());
      context.put("REQUEST_HOST", prefs.hostName == null ? uri.getHost() : prefs.hostName);
      context.put("REQUEST_PORT", prefs.port <= 0 ? uri.getPort() : prefs.port);
      context.put("REQUEST_SCHEME", prefs.scheme == null ? uri.getScheme() : prefs.scheme);
      createMailMessage(context, prefs, result -> {
        if (result.failed()) {
          LOGGER.error("", result.cause());
          MailSendResult msResult = new MailSendResult(result.cause());
          handler.handle(Future.succeededFuture(msResult));
        } else {
          MailMessage email = result.result();
          sendMessage(mailClient, email, handler);
        }
      });
    } catch (Exception e) {
      LOGGER.error("", e);
      MailSendResult msResult = new MailSendResult(e);
      handler.handle(Future.succeededFuture(msResult));
    }
  }

  /**
   * @param email
   * @param sendResult
   */
  private static void sendMessage(MailClient mailClient, MailMessage email,
      Handler<AsyncResult<MailSendResult>> handler) {
    LOGGER.info("Going to send message");
    mailClient.sendMail(email, result -> {
      if (result.failed()) {
        LOGGER.error("", result.cause());
        MailSendResult msResult = new MailSendResult(result);
        handler.handle(Future.succeededFuture(msResult));
      } else {
        MailSendResult msResult = new MailSendResult(result);
        handler.handle(Future.succeededFuture(msResult));
      }
    });
  }

  /**
   * @param context
   * @return
   */
  private static void createMailMessage(RoutingContext context, MailPreferences prefs,
      Handler<AsyncResult<MailMessage>> handler) {
    String mailFrom = prefs.from == null ? readParameter(context, FROM_PARAM, true) : prefs.from;
    MailMessage email = new MailMessage().setFrom(mailFrom);
    if (prefs.bounceAddress != null) {
      email.setBounceAddress(prefs.bounceAddress);
    }
    String to = prefs.to == null || prefs.to.hashCode() == 0 ? readParameterOrContext(context, TO_PARAMETER, null, true)
        : prefs.to;
    email.setTo(to);
    String subject = prefs.subject == null ? readParameterOrContext(context, SUBJECT_PARAMETER, null, false)
        : prefs.subject;
    email.setSubject(subject);
    String text = prefs.text == null || prefs.text.hashCode() == 0
        ? readParameterOrContext(context, TEXT_PARAMETER, null, false) : prefs.text;
    email.setText(text);
    String template = prefs.template == null || prefs.template.hashCode() == 0
        ? readParameterOrContext(context, TEMPLATE_PARAM, null, false) : prefs.template;
    createMailContent(context, prefs, handler, email, template);
  }

  /**
   * @param context
   * @param prefs
   * @param handler
   * @param email
   * @param template
   */
  private static void createMailContent(RoutingContext context, MailPreferences prefs,
      Handler<AsyncResult<MailMessage>> handler, MailMessage email, String template) {
    if (template != null && template.hashCode() != 0) {
      String file = prefs.templateDirectory + "/" + template;
      prefs.templateEngine.render(context, file, res -> {
        if (res.succeeded()) {
          email.setHtml(res.result().toString());
          checkInlineMessage(context, prefs, email, handler);
        } else {
          handler.handle(Future.failedFuture(res.cause()));
        }
      });
    } else {
      String html = prefs.html == null || prefs.html.hashCode() == 0
          ? readParameterOrContext(context, HTML_PARAMETER, null, false) : prefs.html;
      email.setHtml(html);
      checkInlineMessage(context, prefs, email, handler);
    }
  }

  private static void checkInlineMessage(RoutingContext context, MailPreferences prefs, MailMessage msg,
      Handler<AsyncResult<MailMessage>> handler) {
    if (prefs.inline) {
      parseInlineMessage(context, prefs, msg, handler);
    } else {
      handler.handle(Future.succeededFuture(msg));
    }

  }

  /**
   * Replaces src-attributes of img-tags with cid to represent the correct inline-attachment
   * 
   * @param msg
   * @param textValue
   * @param helper
   */
  private static void parseInlineMessage(RoutingContext context, MailPreferences prefs, MailMessage msg,
      Handler<AsyncResult<MailMessage>> handler) {
    List<MailAttachment> attachments = new ArrayList<>();
    StringBuffer result = new StringBuffer();

    // group1: everything before the image src, group2:filename inside the "" of the src element, group4: everything
    // after the image src
    String htmlMessage = msg.getHtml();
    if (htmlMessage != null) {
      Matcher matcher = IMG_PATTERN.matcher(htmlMessage);
      while (matcher.find()) {
        String imageSource = matcher.group(2);
        String cid = getContentId();
        URI imgUrl = makeAbsoluteURI(imageSource);
        if (imgUrl != null) {
          MailAttachment attachment = createAttachment(context, imgUrl, cid);
          matcher.appendReplacement(result, "$1cid:" + cid + "$3");
          attachments.add(attachment);
        } else {
          matcher.appendReplacement(result, "$0");
        }
      }
      matcher.appendTail(result);
    }
    msg.setHtml(result.toString());
    if (attachments.isEmpty()) {
      handler.handle(Future.succeededFuture(msg));
    } else {
      CounterObject co = new CounterObject<>(attachments.size(), handler);
      for (MailAttachment attachment : attachments) {
        readData(prefs, (UriMailAttachment) attachment, rr -> {
          if (rr.failed()) {
            co.setThrowable(rr.cause());
          } else if (co.reduce()) {
            msg.setInlineAttachment(attachments);
            handler.handle(Future.succeededFuture(msg));
          }
        });
        if (co.isError()) {
          break;
        }
      }
    }
  }

  /**
   * Sequence goes from 0 to 100K, then starts up at 0 again. This is large enough,
   * and saves
   * 
   * @return
   */
  public static synchronized int getSeq() {
    return (seq++) % 100000;
  }

  /**
   * One possible way to generate very-likely-unique content IDs.
   * 
   * @return A content id that uses the hostname, the current time, and a sequence number
   *         to avoid collision.
   */
  public static String getContentId() {
    int c = getSeq();
    return c + "." + System.currentTimeMillis() + "@" + hostname;
  }

  private static MailAttachment createAttachment(RoutingContext context, URI uri, String cidName) {
    UriMailAttachment attachment = new UriMailAttachment(uri);
    attachment.setName(cidName);
    attachment.setContentType(getContentType(uri));
    attachment.setDisposition("inline");
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("Content-ID", "<" + cidName + ">");
    attachment.setHeaders(headers);
    return attachment;
  }

  private static void readData(MailPreferences prefs, UriMailAttachment attachment,
      Handler<AsyncResult<Void>> handler) {
    URI uri = attachment.getUri();
    HttpClient client = prefs.httpClient;
    int port = uri.getPort() > 0 ? uri.getPort() : 80;
    HttpClientRequest req = client.request(HttpMethod.GET, port, uri.getHost(), uri.getPath(), resp -> {
      resp.bodyHandler(buff -> {
        try {
          attachment.setData(buff);
          handler.handle(Future.succeededFuture());
        } catch (Exception e) {
          LOGGER.error("", e);
          handler.handle(Future.failedFuture(e));
        }
      });
    });
    req.end();
  }

  private static String getContentType(URI uri) {
    if (uri.getPath().endsWith(".png")) {
      return "image/png";
    }
    return null;
  }

  private static URI makeAbsoluteURI(String url) {
    URI ret = URI.create(url);

    if (ret.isAbsolute()) {
      return ret;
    } else {
      throw new UnsupportedOperationException("image urls must be absolute");
    }
  }

  /**
   * Create a new instance of {@link MailPreferences} with the given properties
   * 
   * @param properties
   *          the properties to be used
   * @return a new instance
   */
  public static MailPreferences createMailPreferences(Vertx vertx, Properties properties) {
    return new MailPreferences(vertx, properties);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.AbstractController#initProperties(java.util.Properties)
   */
  @Override
  public void initProperties(Properties properties) {
    prefs = createMailPreferences(getVertx(), properties);
  }

  /**
   * Creates a default definition for the current instance
   * 
   * @return
   */
  public static RouterDefinition createDefaultRouterDefinition() {
    RouterDefinition def = new RouterDefinition();
    def.setName(MailController.class.getSimpleName());
    def.setBlocking(false);
    def.setController(MailController.class);
    def.setHandlerProperties(getDefaultProperties());
    def.setRoutes(new String[] { "/api/sendmail" });
    return def;
  }

  /**
   * Get the default properties for an implementation of StaticController
   * 
   * @return
   */
  public static Properties getDefaultProperties() {
    Properties json = new Properties();
    json.put(FROM_PARAM, "address@sender.com");
    json.put(INLINE_PROP, "true");
    json.put(ThymeleafTemplateController.TEMPLATE_MODE_PROPERTY, ThymeleafTemplateEngine.DEFAULT_TEMPLATE_MODE);
    json.put(ThymeleafTemplateController.CACHE_ENABLED_PROPERTY, "true");
    json.put(ThymeleafTemplateController.TEMPLATE_DIRECTORY_PROPERTY,
        ThymeleafTemplateController.DEFAULT_TEMPLATE_DIRECTORY);
    return json;
  }

  /**
   * Preferences, which are defining the behaviour of the MailController
   * 
   * @author Michael Remme
   *
   */
  public static class MailPreferences {
    private String to;
    private String from = null;
    private String subject = null;
    private String bounceAddress = null;
    private ThymeleafTemplateEngine templateEngine;
    private String templateDirectory;
    private String template;
    private String html;
    private String text;
    private boolean inline = true;
    private HttpClient httpClient;
    private String hostName = null;
    private int port = -1;
    private String scheme = null;

    /**
     * 
     */
    MailPreferences(Vertx vertx, Properties props) {
      from = readProperty(props, FROM_PARAM, null, false);
      bounceAddress = readProperty(props, BOUNCE_ADDRESS_PARAM, null, false);
      to = readProperty(props, TO_PARAMETER, null, false);
      subject = readProperty(props, SUBJECT_PARAMETER, null, false);
      template = readProperty(props, TEMPLATE_PARAM, null, false);
      templateEngine = ThymeleafTemplateController.createTemplateEngine(props);
      templateDirectory = ThymeleafTemplateController.getTemplateDirectory(props);
      html = readProperty(props, HTML_PARAMETER, "", false);
      text = readProperty(props, TEXT_PARAMETER, "", false);
      inline = Boolean.valueOf(readProperty(props, INLINE_PROP, "true", false));
      httpClient = vertx.createHttpClient();
      hostName = readProperty(props, HOSTNAME_PROP, null, false);
      port = Integer.parseInt(readProperty(props, PORT_PROP, "-1", false));
      scheme = readProperty(props, SCHEME_PROP, null, false);
    }

    /**
     * Get the defined recipient
     * 
     * @return the to
     */
    public final String getTo() {
      return to;
    }

    /**
     * get the defined sender
     * 
     * @return the from
     */
    public final String getFrom() {
      return from;
    }

    /**
     * get the defined subject
     * 
     * @return the subject
     */
    public final String getSubject() {
      return subject;
    }

    /**
     * get the defined bouncer address
     * 
     * @return the bounceAddress
     */
    public final String getBounceAddress() {
      return bounceAddress;
    }

    /**
     * get the defined template directory
     * 
     * @return the templateDirectory
     */
    public final String getTemplateDirectory() {
      return templateDirectory;
    }

    /**
     * get the path of a template to be used to create the content
     * 
     * @return the template
     */
    public final String getTemplate() {
      return template;
    }

    /**
     * get a static html text to be sent
     * 
     * @return the html
     */
    public final String getHtml() {
      return html;
    }

    /**
     * get a static text to be sent
     * 
     * @return the text
     */
    public final String getText() {
      return text;
    }

    /**
     * shall images be sent inline?
     * 
     * @return the inline
     */
    public final boolean isInline() {
      return inline;
    }

    /**
     * get the name of a defined host, which can be used inside templates to build links
     * 
     * @return the hostName
     */
    public final String getHostName() {
      return hostName;
    }

    /**
     * get the name of a defined port, which can be used inside templates to build links
     * 
     * @return the port
     */
    public final int getPort() {
      return port;
    }

    /**
     * get the name of a defined scheme, which can be used inside templates to build links
     * 
     * @return the scheme
     */
    public final String getScheme() {
      return scheme;
    }

  }

  /**
   * The result of a mail composing and sending
   * 
   * @author Michael Remme
   *
   */
  public static class MailSendResult {
    public boolean success = false;
    public String errorMessage;
    public MailResult mailResult;

    MailSendResult(Throwable exception) {
      success = false;
      errorMessage = exception.toString();
    }

    MailSendResult(AsyncResult<MailResult> result) {
      if (result.failed()) {
        success = false;
        errorMessage = result.cause().toString();
      } else {
        success = true;
        mailResult = result.result();
      }
    }
  }
}
