package com.toshi.manager

import com.toshi.extensions.getQueryMap
import com.toshi.extensions.isGroupId
import com.toshi.manager.network.IdService
import com.toshi.manager.store.BlockedUserStore
import com.toshi.manager.store.GroupStore
import com.toshi.manager.store.UserStore
import com.toshi.model.local.BlockedUser
import com.toshi.model.local.Group
import com.toshi.model.local.Recipient
import com.toshi.model.local.Report
import com.toshi.model.local.User
import com.toshi.model.network.SearchResult
import com.toshi.model.network.ServerTime
import com.toshi.model.network.UserSection
import com.toshi.model.network.user.UserType
import com.toshi.util.logging.LogUtil
import com.toshi.view.BaseApplication
import rx.Completable
import rx.Observable
import rx.Scheduler
import rx.Single
import rx.schedulers.Schedulers
import java.io.IOException

class RecipientManager(
        private val idService: IdService = IdService.get(),
        private val groupStore: GroupStore = GroupStore(),
        private val userStore: UserStore = UserStore(),
        private val blockedUserStore: BlockedUserStore = BlockedUserStore(),
        private val baseApplication: BaseApplication = BaseApplication.get(),
        private val scheduler: Scheduler = Schedulers.io()
) {

    fun getFromId(recipientId: String): Single<Recipient> {
        return if (recipientId.isGroupId()) getGroupFromId(recipientId).map { Recipient(it) }
        else getUserFromToshiId(recipientId).map { Recipient(it) }
    }

    fun getGroupFromId(id: String): Single<Group> {
        return groupStore.loadForId(id)
                .subscribeOn(scheduler)
                .doOnError { LogUtil.exception("getGroupFromId", it) }
    }

    fun getUserFromUsername(username: String): Single<User> {
        return Single
                .concat(
                        userStore.loadForUsername(username),
                        fetchAndCacheFromNetworkByUsername(username))
                .subscribeOn(scheduler)
                .first { isUserFresh(it) }
                .doOnError { LogUtil.exception("getUserFromUsername", it) }
                .toSingle()
    }

    fun getUserFromToshiId(toshiId: String): Single<User> {
        return Single
                .concat(
                        userStore.loadForToshiId(toshiId),
                        fetchAndCacheFromNetworkByToshiId(toshiId))
                .subscribeOn(scheduler)
                .first { isUserFresh(it) }
                .doOnError { LogUtil.exception("getUserFromToshiId", it) }
                .toSingle()
    }

    private fun isUserFresh(user: User?): Boolean {
        return when {
            user == null -> false
            baseApplication.isConnected -> true
            else -> !user.needsRefresh()
        }
    }

    fun getUserFromPaymentAddress(paymentAddress: String): Single<User> {
        return Single
                .concat(
                        userStore.loadForPaymentAddress(paymentAddress),
                        fetchAndCacheFromNetworkByPaymentAddress(paymentAddress).toSingle()
                )
                .subscribeOn(scheduler)
                .first { isUserFresh(it) }
                .doOnError { LogUtil.exception("getUserFromPaymentAddress", it) }
                .toSingle()
    }

    private fun fetchAndCacheFromNetworkByUsername(username: String): Single<User> {
        // It's the same endpoint
        return fetchAndCacheFromNetworkByToshiId(username)
    }

    private fun fetchAndCacheFromNetworkByToshiId(userAddress: String): Single<User> {
        return idService
                .api
                .getUser(userAddress)
                .subscribeOn(scheduler)
                .doOnSuccess { cacheUser(it) }
    }

    fun fetchUsersFromToshiIds(userIds: List<String>): Single<List<User>> {
        return idService
                .api
                .getUsers(userIds)
                .map { it.results }
                .subscribeOn(scheduler)
    }

    private fun fetchAndCacheFromNetworkByPaymentAddress(paymentAddress: String): Observable<User> {
        return idService
                .api
                .searchByPaymentAddress(paymentAddress)
                .toObservable()
                .filter { it.results.size > 0 }
                .map { it.results[0] }
                .subscribeOn(scheduler)
                .doOnNext { cacheUser(it) }
                .doOnError { LogUtil.exception("fetchAndCacheFromNetworkByPaymentAddress", it) }
    }

    fun cacheUser(user: User) {
        userStore.save(user)
                .subscribe(
                        { },
                        { LogUtil.exception("Error while saving user to db", it) }
                )
    }

    fun searchOnlineUsers(query: String): Single<List<User>> {
        return idService
                .api
                .searchBy(UserType.USER.name.toLowerCase(), query)
                .subscribeOn(scheduler)
                .map { it.results }
    }

    fun isUserBlocked(ownerAddress: String): Single<Boolean> {
        return blockedUserStore
                .isBlocked(ownerAddress)
                .subscribeOn(scheduler)
    }

    fun blockUser(ownerAddress: String): Completable {
        val blockedUser = BlockedUser()
                .setOwnerAddress(ownerAddress)
        return Completable
                .fromAction { blockedUserStore.save(blockedUser) }
                .subscribeOn(scheduler)
    }

    fun unblockUser(ownerAddress: String): Completable {
        return Completable
                .fromAction { blockedUserStore.delete(ownerAddress) }
                .subscribeOn(scheduler)
    }

    fun reportUser(report: Report): Completable {
        return getTimestamp()
                .flatMapCompletable { idService.api.reportUser(report, it.get()) }
                .subscribeOn(scheduler)
    }

    fun searchForUsersWithType(type: String, query: String? = null): Single<SearchResult<User>> {
        return idService
                .api
                .search(type, query)
                .doOnSuccess { cacheUsers(it.results) }
                .subscribeOn(scheduler)
    }

    fun searchForUsersWithQuery(query: String): Single<SearchResult<User>> {
        val queryMap = query.getQueryMap()
        return idService
                .api
                .search(queryMap)
                .doOnSuccess { cacheUsers(it.results) }
                .subscribeOn(scheduler)
    }

    fun getPopularSearches(): Single<List<UserSection>> {
        return idService
                .api
                .getPopularSearches()
                .map { it.sections }
                .doOnSuccess { it.forEach { cacheUsers(it.results) } }
                .subscribeOn(scheduler)
    }

    private fun cacheUsers(users: List<User>) {
        userStore.saveUsers(users)
                .subscribe(
                        { },
                        { LogUtil.exception("Error while saving users to db", it) }
                )
    }

    fun getTimestamp(): Single<ServerTime> = idService.api.timestamp

    fun clear() = clearCache()

    private fun clearCache() {
        try {
            idService.clearCache()
        } catch (e: IOException) {
            LogUtil.exception("Error while clearing network cache", e)
        }
    }
}