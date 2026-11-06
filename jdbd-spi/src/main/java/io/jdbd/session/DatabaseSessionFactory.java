/*
 * Copyright 2023-2043 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jdbd.session;

import io.jdbd.Driver;
import io.jdbd.DriverVersion;
import io.jdbd.lang.Nullable;
import org.reactivestreams.Publisher;

import java.util.Map;
import java.util.function.Function;


/**
 * <p>This interface representing the factory that create {@link DatabaseSession} by following methods:
 *     <ul>
 *         <li>{@link #localSession()}</li>
 *         <li>{@link #rmSession()}</li>
 *         <li>{@link #physicalLocalSession()}</li>
 *         <li>{@link #physicalRmSession()}</li>
 *     </ul>
 * <br/>
 * <p>The instance of this interface is created by :
 * <ul>
 *     <li>{@link Driver#forDeveloper(String, Map)}</li>
 *     <li>{@link Driver#forPoolVendor(String, Map)}</li>
 *     <li>pool vendor</li>
 * </ul>
 * <p>This interface is base interface of {@link io.jdbd.pool.ReadWriteSplittingFactory}.
 *
 * @since 1.0
 */
public interface DatabaseSessionFactory extends OptionSpec, Closeable {

    /**
     * <p>
     * This method return factory name ,if you don't specified then return 'unnamed' .
     * <br/>
     *
     * @return factory name. see {@link io.jdbd.Driver#FACTORY_NAME}
     */
    String name();


    /**
     * <p>This method is equivalent to following :
     * <pre>
     *         <code><br/>
     *             // factory is instance of DatabaseSessionFactory
     *             factory.localSession(null,Option.EMPTY_OPTION_FUNC) ;
     *         </code>
     * </pre>
     *
     * @see #localSession(String, Function)
     */
    Publisher<LocalDatabaseSession> localSession();

    /**
     * <p>
     * Get the instance of {@link LocalDatabaseSession}.
     * <br/>
     * <p>
     * <strong>NOTE</strong> : driver don't send message to database server before subscribing.
     * Driver developer must guarantee this feature.
     * <br/>
     *
     * @param name       optional session name,if null ,then {@link DatabaseSession#name()} return 'unnamed' .
     * @param optionFunc option function ,if don't support ,then ignore
     * @return emit just one {@link LocalDatabaseSession} instance or {@link Throwable}. Like {@code reactor.core.publisher.Mono}.
     * <ul>
     * <li>If the instance of {@link DatabaseSessionFactory} is created pool vendor , then always emit non-{@link io.jdbd.pool.PoolLocalDatabaseSession} instance.</li>
     * <li>Else if the instance of {@link DatabaseSessionFactory} is created driver vendor ,then :
     *      <ul>
     *          <li>If the instance of {@link DatabaseSessionFactory} is created {@link Driver#forPoolVendor(String, Map)}, then always emit {@link io.jdbd.pool.PoolLocalDatabaseSession} instance.</li>
     *          <li>Else always emit non-{@link io.jdbd.pool.PoolLocalDatabaseSession} instance.</li>
     *      </ul>
     * </li>
     * <li>Else emit {@link LocalDatabaseSession} instance.</li>
     * </ul>
     * @throws io.jdbd.JdbdException emit(not throw) when
     *                               <ul>
     *                                   <li>this {@link DatabaseSessionFactory} have closed.</li>
     *                                   <li>network error</li>
     *                                   <li>server response error message,see {@link io.jdbd.result.ServerException}</li>
     *                               </ul>
     */
    Publisher<LocalDatabaseSession> localSession(@Nullable String name, Function<Option<?>, ?> optionFunc);

    /**
     * <p>This method is equivalent to following :
     * <pre>
     *         <code><br/>
     *             // factory is instance of DatabaseSessionFactory
     *             factory.physicalLocalSession(null,Option.EMPTY_OPTION_FUNC) ;
     *         </code>
     * </pre>
     *
     * @see #physicalLocalSession(String, Function)
     */
    Publisher<LocalDatabaseSession> physicalLocalSession();

    /**
     * <p>
     * Get the instance of {@link LocalDatabaseSession} that is bound to one physical connection, never a
     * connection pool proxy.
     * <br/>
     * <p>
     * <strong>NOTE</strong> : driver don't send message to database server before subscribing.
     * Driver developer must guarantee this feature.
     * <br/>
     * <p>
     * Unlike {@link #localSession(String, Function)}, the session emitted by this method is
     * <strong>never</strong> a pool proxy, whatever the vendor of this {@link DatabaseSessionFactory} is :
     * <ul>
     *     <li>never emit {@link io.jdbd.pool.PoolLocalDatabaseSession} instance.</li>
     *     <li>{@link DatabaseSession#close()} physically closes the underlying connection, the connection is
     *     neither returned to the pool nor reset for next use.</li>
     * </ul>
     * <p>
     * This method is designed for the features that rely on the state of one particular physical connection,
     * for example :
     * <ul>
     *     <li>PostgreSQL session level advisory lock, eg : {@code pg_try_advisory_lock(bigint)}, the lock is
     *     held by the backend session of this physical connection, and is released only by
     *     {@code pg_advisory_unlock(bigint)} or by the end of this session.</li>
     *     <li>PostgreSQL {@code LISTEN}/{@code NOTIFY}, temporary table , session variables.</li>
     * </ul>
     * <p>
     * The life cycle of the returned session and its physical connection is owned by the caller, caller must
     * close the session after use, otherwise the connection will leak.
     *
     * @param name       optional session name,if null ,then {@link DatabaseSession#name()} return 'unnamed' .
     * @param optionFunc option function ,if don't support ,then ignore
     * @return emit just one {@link LocalDatabaseSession} instance or {@link Throwable}. Like {@code reactor.core.publisher.Mono}.
     * <ul>
     * <li>If this {@link DatabaseSessionFactory} support physical session, then emit a non-{@link io.jdbd.pool.PoolLocalDatabaseSession}
     * instance that is bound to one physical connection.</li>
     * <li>Else emit {@link io.jdbd.JdbdException}.</li>
     * </ul>
     * @throws io.jdbd.JdbdException emit(not throw) when
     *                               <ul>
     *                                   <li>driver or pool vendor don't support this method.</li>
     *                                   <li>this {@link DatabaseSessionFactory} have closed.</li>
     *                                   <li>network error</li>
     *                                   <li>server response error message,see {@link io.jdbd.result.ServerException}</li>
     *                               </ul>
     * @see #localSession(String, Function)
     */
    Publisher<LocalDatabaseSession> physicalLocalSession(@Nullable String name, Function<Option<?>, ?> optionFunc);


    /**
     * <p>This method is equivalent to following :
     * <pre>
     *         <code><br/>
     *             // factory is instance of DatabaseSessionFactory
     *             factory.rmSession(null,Option.EMPTY_OPTION_FUNC) ;
     *         </code>
     * </pre>
     *
     * @see #rmSession(String, Function)
     */
    Publisher<RmDatabaseSession> rmSession();

    /**
     * <p>
     * Get the instance of {@link RmDatabaseSession}.
     * <br/>
     * <p>
     * <strong>NOTE</strong> : driver don't send message to database server before subscribing.
     * Driver developer must guarantee this feature.
     * <br/>
     *
     * @param name       optional session name,if null ,then {@link DatabaseSession#name()} return 'unnamed' .
     * @param optionFunc option function ,if don't support ,then ignore
     * @return emit just one {@link RmDatabaseSession} instance or {@link Throwable}. Like {@code reactor.core.publisher.Mono}.
     * <ul>
     * <li>If the instance of {@link DatabaseSessionFactory} is created pool vendor , then always emit non-{@link io.jdbd.pool.PoolRmDatabaseSession} instance.</li>
     * <li>Else if the instance of {@link DatabaseSessionFactory} is created driver vendor ,then :
     *      <ul>
     *          <li>If the instance of {@link DatabaseSessionFactory} is created {@link Driver#forPoolVendor(String, Map)}, then always emit {@link io.jdbd.pool.PoolRmDatabaseSession} instance.</li>
     *          <li>Else always emit non-{@link io.jdbd.pool.PoolRmDatabaseSession} instance.</li>
     *      </ul>
     * </li>
     * <li>Else emit {@link RmDatabaseSession} instance.</li>
     * </ul>
     * @throws io.jdbd.JdbdException emit(not throw) when
     *                               <ul>
     *                                   <li>driver don't support this method</li>
     *                                   <li>this {@link DatabaseSessionFactory} have closed.</li>
     *                                   <li>network error</li>
     *                                   <li>server response error message,see {@link io.jdbd.result.ServerException}</li>
     *                               </ul>
     */
    Publisher<RmDatabaseSession> rmSession(@Nullable String name, Function<Option<?>, ?> optionFunc);

    /**
     * <p>This method is equivalent to following :
     * <pre>
     *         <code><br/>
     *             // factory is instance of DatabaseSessionFactory
     *             factory.physicalRmSession(null,Option.EMPTY_OPTION_FUNC) ;
     *         </code>
     * </pre>
     *
     * @see #physicalRmSession(String, Function)
     */
    Publisher<RmDatabaseSession> physicalRmSession();

    /**
     * <p>
     * Get the instance of {@link RmDatabaseSession} that is bound to one physical connection, never a
     * connection pool proxy.
     * <br/>
     * <p>
     * <strong>NOTE</strong> : driver don't send message to database server before subscribing.
     * Driver developer must guarantee this feature.
     * <br/>
     * <p>
     * Unlike {@link #rmSession(String, Function)}, the session emitted by this method is
     * <strong>never</strong> a pool proxy, whatever the vendor of this {@link DatabaseSessionFactory} is :
     * <ul>
     *     <li>never emit {@link io.jdbd.pool.PoolRmDatabaseSession} instance.</li>
     *     <li>{@link DatabaseSession#close()} physically closes the underlying connection, the connection is
     *     neither returned to the pool nor reset for next use.</li>
     * </ul>
     * <p>
     * This method is designed for the resource manager features that rely on the state of one particular
     * physical connection, for example :
     * <ul>
     *     <li>PostgreSQL session level advisory lock, eg : {@code pg_try_advisory_lock(bigint)}, the lock is
     *     held by the backend session of this physical connection, and is released only by
     *     {@code pg_advisory_unlock(bigint)} or by the end of this session.</li>
     *     <li>PostgreSQL {@code LISTEN}/{@code NOTIFY}, temporary table , session variables.</li>
     * </ul>
     * <p>
     * The life cycle of the returned session and its physical connection is owned by the caller, caller must
     * close the session after use, otherwise the connection will leak.
     *
     * @param name       optional session name,if null ,then {@link DatabaseSession#name()} return 'unnamed' .
     * @param optionFunc option function ,if don't support ,then ignore
     * @return emit just one {@link RmDatabaseSession} instance or {@link Throwable}. Like {@code reactor.core.publisher.Mono}.
     * <ul>
     * <li>If this {@link DatabaseSessionFactory} support physical session, then emit a non-{@link io.jdbd.pool.PoolRmDatabaseSession}
     * instance that is bound to one physical connection.</li>
     * <li>Else emit {@link io.jdbd.JdbdException}.</li>
     * </ul>
     * @throws io.jdbd.JdbdException emit(not throw) when
     *                               <ul>
     *                                   <li>driver or pool vendor don't support this method.</li>
     *                                   <li>this {@link DatabaseSessionFactory} have closed.</li>
     *                                   <li>network error</li>
     *                                   <li>server response error message,see {@link io.jdbd.result.ServerException}</li>
     *                               </ul>
     * @see #rmSession(String, Function)
     */
    Publisher<RmDatabaseSession> physicalRmSession(@Nullable String name, Function<Option<?>, ?> optionFunc);


    /**
     * @return database product family,For example :  MySQL , PostgreSQL.
     */
    String productFamily();


    /**
     * @return session factory vendor,The value returned typically is the package name for this vendor.
     * The session factory vendor possibly is pool vendor.
     */
    String factoryVendor();

    /**
     * @return driver vendor,The value returned typically is the package name for this vendor.
     * @see Driver#vendor()
     */
    String driverVendor();

    /**
     * @see Driver#version()
     */
    DriverVersion driverVersion();

    /**
     * override {@link Object#toString()}
     *
     * @return driver info, contain : <ol>
     * <li>{@link #name()}</li>
     * <li>{@link #factoryVendor()}</li>
     * <li>{@link #driverVersion()}</li>
     * <li>{@link #productFamily()}</li>
     * <li>{@link #driverVersion()}</li>
     * <li>{@link System#identityHashCode(Object)}</li>
     * </ol>
     */
    @Override
    String toString();


}
