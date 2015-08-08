/*
 * Copyright 2015 Norbert Potocki (norbert.potocki@nort.pl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cfg4j.provider;

import static java.util.Objects.requireNonNull;

import com.github.drapostolos.typeparser.NoSuchRegisteredParserException;
import com.github.drapostolos.typeparser.TypeParser;
import com.github.drapostolos.typeparser.TypeParserException;
import org.cfg4j.source.ConfigurationSource;
import org.cfg4j.source.context.environment.Environment;
import org.cfg4j.source.context.environment.MissingEnvironmentException;
import org.cfg4j.validator.BindingValidator;

import java.lang.reflect.Proxy;
import java.util.NoSuchElementException;
import java.util.Properties;

/**
 * Basic implementation of {@link ConfigurationProvider}. To construct this provider use {@link ConfigurationProviderBuilder}.
 */
class SimpleConfigurationProvider implements ConfigurationProvider {

  private final ConfigurationSource configurationSource;
  private final Environment environment;

  /**
   * {@link ConfigurationProvider} backed by provided {@link ConfigurationSource} and using {@code environment}
   * to select environment. To construct this provider use {@link ConfigurationProviderBuilder}.
   *
   * @param configurationSource source for configuration
   * @param environment         {@link Environment} to use
   */
  SimpleConfigurationProvider(ConfigurationSource configurationSource, Environment environment) {
    this.configurationSource = requireNonNull(configurationSource);
    this.environment = requireNonNull(environment);
  }

  @Override
  public Properties allConfigurationAsProperties() {
    try {
      return configurationSource.getConfiguration(environment);
    } catch (IllegalStateException | MissingEnvironmentException e) {
      throw new IllegalStateException("Couldn't fetch configuration from configuration source", e);
    }
  }

  @Override
  public <T> T getProperty(String key, Class<T> type) {
    String propertyStr = getProperty(key);

    try {
      TypeParser parser = TypeParser.newBuilder().build();
      return parser.parse(propertyStr, type);
    } catch (TypeParserException | NoSuchRegisteredParserException e) {
      throw new IllegalArgumentException("Unable to cast value \'" + propertyStr + "\' to " + type, e);
    }
  }

  @Override
  public <T> T getProperty(String key, GenericTypeInterface genericType) {
    String propertyStr = getProperty(key);

    try {
      TypeParser parser = TypeParser.newBuilder().build();
      @SuppressWarnings("unchecked")
      T property = (T) parser.parseType(propertyStr, genericType.getType());
      return property;
    } catch (TypeParserException | NoSuchRegisteredParserException e) {
      throw new IllegalArgumentException("Unable to cast value \'" + propertyStr + "\' to " + genericType, e);
    }
  }

  private String getProperty(String key) {
    try {

      String property = configurationSource.getConfiguration(environment).getProperty(key);

      if (property == null) {
        throw new NoSuchElementException("No configuration with key: " + key);
      }

      return property;

    } catch (IllegalStateException e) {
      throw new IllegalStateException("Couldn't fetch configuration from configuration source for key: " + key, e);
    }
  }

  @Override
  public <T> T bind(String prefix, Class<T> type) {
    @SuppressWarnings("unchecked")
    T proxy = (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, new BindInvocationHandler(this, prefix));

    new BindingValidator().validate(proxy, type);

    return proxy;
  }

  @Override
  public String toString() {
    return "SimpleConfigurationProvider{" +
        "configurationSource=" + configurationSource +
        ", environment=" + environment +
        '}';
  }
}
