package com.hacybeyker.cartera.core.data.market

import com.hacybeyker.cartera.core.data.market.di.MarketDataModule
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketStreamUrlTest {

    @Test
    fun `the stream url hangs from the API base url`() {
        assertEquals(
            "http://127.0.0.1:8765/api/v1/market/stream",
            MarketDataModule.provideMarketStreamUrl().toString()
        )
    }
}
