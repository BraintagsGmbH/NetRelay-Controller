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
package de.braintags.netrelay.controller.authentication;

import java.util.Properties;

import de.braintags.netrelay.controller.AbstractController;
import de.braintags.netrelay.controller.authentication.authprovider.CustomAuthProvider;
import de.braintags.vertx.auth.datastore.IDatastoreAuth;
import de.braintags.vertx.jomnigate.IDataStore;
import de.braintags.vertx.jomnigate.mapping.IMapper;
import de.braintags.vertx.jomnigate.mapping.IProperty;
import de.braintags.vertx.jomnigate.mongo.MongoDataStore;
import de.braintags.vertx.util.exception.InitException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.mongo.HashSaltStyle;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.mongo.MongoClient;

/**
 * An abstract implementation of IController, which initializes an {@link AuthProvider} to be used to perform
 * authentication and authorization in extending controllers
 * 
 * Config-Parameter:<br/>
 * possible parameters, which are read from the configuration
 * <ul>
 * <li>{@value #AUTH_PROVIDER_PROP}</li>
 * <ul>
 * <li>for {@link MongoAuth}, specific parameters for MongoAuth can be added, like
 * <ul>
 * <li>{@link MongoAuth#PROPERTY_COLLECTION_NAME}</li>
 * <li>{@link MongoAuth#PROPERTY_PASSWORD_FIELD}</li>
 * <li>{@link MongoAuth#PROPERTY_USERNAME_FIELD}</li>
 * <li>{@link MongoAuth#PROPERTY_ROLE_FIELD}</li>
 * </ul>
 * </li>
 * <li>for {@value #AUTH_PROVIDER_CUSTOM}, the parameter {@value #AUTH_PROVIDER_CUSTOM_CLASS} must be filled with the
 * class of the auth provider</li>
 * </ul>
 * <li>{@link #USERNAME_FIELD}</li>
 * <li>{@link #PASSWORD_FIELD}</li>
 * </ul>
 * <br>
 * 
 * Request-Parameter:<br/>
 * possible parameters, which are read from a request
 * <UL>
 * <LI>none
 * </UL>
 * <br/>
 * 
 * Result-Parameter:<br/>
 * possible paramters, which will be placed into the context
 * <UL>
 * <LI>none
 * </UL>
 * <br/>
 * 
 * 
 * @author Michael Remme
 * 
 */
public abstract class AbstractAuthProviderController extends AbstractController {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(AbstractAuthProviderController.class);

  /**
   * Used as possible value for property {@link #AUTH_PROVIDER_PROP} and references to an authentication provider
   * connected to a mongo db
   */
  public static final String AUTH_PROVIDER_MONGO = "MongoAuth";

  /**
   * Used as possible value for property {@link #AUTH_PROVIDER_PROP} and references to an authentication provider
   * connected to the internal IDatastoreContainer
   */
  public static final String AUTH_PROVIDER_DATASTORE = "AuthProviderDatastore";

  /**
   * Used if a custom {@link AuthProvider} should be used
   */
  public static final String AUTH_PROVIDER_CUSTOM = "CustomAuthProvider";
  /**
   * Property for the class name that implements CustomAuthProvider if the configured provider is
   * {@link #AUTH_PROVIDER_CUSTOM}
   */
  public static final String AUTH_PROVIDER_CUSTOM_CLASS = "customAuthProviderClass";

  /**
   * The name of the key, which is used, to store the name of the mapper in the {@link User#principal()}
   */
  public static final String MAPPERNAME_IN_PRINCIPAL = "mapper";

  /**
   * Defines the name of the {@link AuthProvider} to be used. Currently {@link #AUTH_PROVIDER_MONGO} is supported
   */
  public static final String AUTH_PROVIDER_PROP = "authProvider";

  /**
   * Defines the name of the parameter where the username is stored in a login request
   */
  public static final String USERNAME_FIELD = "usernameField";

  /**
   * Defines the name of the parameter where the password is stored in a login request
   */
  public static final String PASSWORD_FIELD = "passwordField";

  private static AuthProvider authProvider;

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.AbstractController#initProperties(java.util.Properties)
   */
  @Override
  public void initProperties(Properties properties) {
    this.authProvider = createAuthProvider(properties);
  }

  /**
   * Get the initialized instance of {@link AuthProvider}
   * 
   * @return the {@link AuthProvider}
   */
  protected AuthProvider getAuthProvider() {
    return authProvider;
  }

  protected AuthProvider createAuthProvider(Properties properties) {
    String tmpAuthProvider = readProperty(AUTH_PROVIDER_PROP, AUTH_PROVIDER_DATASTORE, false);
    if (tmpAuthProvider.equals(AUTH_PROVIDER_MONGO)) {
      String mapper = readProperty(MongoAuth.PROPERTY_COLLECTION_NAME, null, true);
      return new AuthProviderProxy(initMongoAuthProvider(mapper), mapper);
    } else if (tmpAuthProvider.equals(AUTH_PROVIDER_DATASTORE)) {
      String mapper = readProperty(MongoAuth.PROPERTY_COLLECTION_NAME, null, true);
      return new AuthProviderProxy(initDatastoreAuthProvider(mapper), mapper);
    } else if (tmpAuthProvider.equals(AUTH_PROVIDER_CUSTOM)) {
      String className = readProperty(AUTH_PROVIDER_CUSTOM_CLASS, null, true);
      try {
        CustomAuthProvider provider = (CustomAuthProvider) Class.forName(className).newInstance();
        provider.init(properties, getNetRelay());
        return provider;
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
        throw new InitException("Could not create custom auth provider " + className, e);
      }
    } else {
      throw new UnsupportedOperationException("unsupported authprovider: " + tmpAuthProvider);
    }
  }

  private AuthProvider initDatastoreAuthProvider(String mapper) {
    Class mapperClass = getNetRelay().getSettings().getMappingDefinitions().getMapperClass(mapper);
    if (mapperClass == null) {
      throw new InitException("Could not find defined mapper class for mapper '" + mapper + "'");
    }
    JsonObject config = new JsonObject();
    config.put(IDatastoreAuth.PROPERTY_MAPPER_CLASS_NAME, mapperClass.getName());
    return IDatastoreAuth.create(getNetRelay().getDatastore(), config);
  }

  /**
   * Init the Authentication Service
   */
  private AuthProvider initMongoAuthProvider(String mapper) {
    IDataStore store = getNetRelay().getDatastore();
    if (!(store instanceof MongoDataStore)) {
      throw new IllegalArgumentException("MongoAuthProvider expects a MongoDataStore");
    }
    JsonObject config = new JsonObject();
    String saltStyle = readProperty(MongoAuth.PROPERTY_SALT_STYLE, HashSaltStyle.NO_SALT.toString(), false);
    config.put(MongoAuth.PROPERTY_SALT_STYLE, HashSaltStyle.valueOf(saltStyle));

    MongoAuth auth = MongoAuth.create((MongoClient) ((MongoDataStore) store).getClient(), config);

    String passwordFieldName = readProperty(PASSWORD_FIELD, null, true);
    Class mapperClass = getNetRelay().getSettings().getMappingDefinitions().getMapperClass(mapper);
    if (mapperClass == null) {
      throw new InitException("Could not find mapper with name " + mapper);
    }
    IMapper mapperDef = getNetRelay().getDatastore().getMapperFactory().getMapper(mapperClass);
    IProperty pwField = mapperDef.getField(passwordFieldName);
    if (pwField.getEncoder() != null) {
      throw new InitException("MongoAuth does not support the annotation Encoder, please use DatastoreAuth instead");
    }
    auth.setPasswordField(passwordFieldName);
    auth.setUsernameField(readProperty(USERNAME_FIELD, null, true));
    auth.setCollectionName(mapper);

    String roleField = readProperty(MongoAuth.PROPERTY_ROLE_FIELD, null, false);
    if (roleField != null) {
      auth.setRoleField(roleField);
    }
    String saltField = readProperty(MongoAuth.PROPERTY_SALT_FIELD, null, false);
    if (saltField != null) {
      auth.setSaltField(saltField);
    }

    String authCredentialField = readProperty(MongoAuth.PROPERTY_CREDENTIAL_USERNAME_FIELD, null, false);
    if (authCredentialField != null) {
      auth.setUsernameCredentialField(authCredentialField);
    }

    String authPasswordCredField = readProperty(MongoAuth.PROPERTY_CREDENTIAL_PASSWORD_FIELD, null, false);
    if (authPasswordCredField != null) {
      auth.setPasswordCredentialField(authPasswordCredField);
    }

    return auth;
  }

  class AuthProviderProxy implements AuthProvider {
    AuthProvider prov;
    String mapper;

    AuthProviderProxy(AuthProvider prov, String mapper) {
      this.prov = prov;
      this.mapper = mapper;
    }

    /*
     * (non-Javadoc)
     * 
     * @see io.vertx.ext.auth.AuthProvider#authenticate(io.vertx.core.json.JsonObject, io.vertx.core.Handler)
     */
    @Override
    public void authenticate(JsonObject arg0, Handler<AsyncResult<User>> handler) {
      prov.authenticate(arg0, result -> {
        if (result.failed()) {
          LOGGER.info("Authentication failed: " + result.cause());
          handler.handle(result);
        } else {
          User user = result.result();
          user.principal().put(MAPPERNAME_IN_PRINCIPAL, mapper);
          handler.handle(Future.succeededFuture(user));
        }
      });
    }

    /**
     * Get the internal instance of {@link AuthProvider} to access specific configuration infos
     * 
     * @return the internal provider
     */
    public AuthProvider getProvider() {
      return prov;
    }

  }

}
