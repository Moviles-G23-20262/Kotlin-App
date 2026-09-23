package com.campusswap.app

import android.content.Context
import com.campusswap.app.data.ExchangeRepository
import com.campusswap.app.data.location.FusedLocationDataSource
import com.campusswap.app.data.location.LocationDataSource
import com.campusswap.app.data.remote.ApiClient
import com.campusswap.app.data.remote.RetrofitExchangeRemoteDataSource

class AppContainer(context: Context) {
    private val api = ApiClient.create()

    val locationDataSource: LocationDataSource = FusedLocationDataSource(context)

    val exchangeRepository = ExchangeRepository(RetrofitExchangeRemoteDataSource(api))
}
