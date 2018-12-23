package com.gdavidpb.tuindice.domain.usecase

import com.gdavidpb.tuindice.domain.model.Account
import com.gdavidpb.tuindice.domain.repository.LocalDatabaseRepository
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

open class GetAccountUseCase(
        private val databaseRepository: LocalDatabaseRepository
) : MaybeUseCase<Account, Void?>(
        Schedulers.io(),
        AndroidSchedulers.mainThread()
) {
    override fun buildUseCaseObservable(params: Void?): Maybe<Account> {
        return databaseRepository.getActiveAccount()
    }
}