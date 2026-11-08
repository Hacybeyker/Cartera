package com.hacybeyker.cartera.legacy.customer

import com.hacybeyker.cartera.legacy.customer.domain.CodeCheck
import com.hacybeyker.cartera.legacy.customer.domain.ValidateCustomerCodeUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateCustomerCodeUseCaseTest {
    private val validate = ValidateCustomerCodeUseCase()

    @Test
    fun `a code that fits the format of the biller is valid`() {
        assertEquals(CodeCheck.VALID, validate("100200", "^\\d{6}$"))
    }

    @Test
    fun `an empty code is empty before its format is checked`() {
        assertEquals(CodeCheck.EMPTY, validate("", "^\\d{6}$"))
    }

    @Test
    fun `a code with the wrong length or characters does not fit the format`() {
        assertEquals(CodeCheck.INVALID_FORMAT, validate("10020", "^\\d{6}$"))
        assertEquals(CodeCheck.INVALID_FORMAT, validate("1002001", "^\\d{6}$"))
        assertEquals(CodeCheck.INVALID_FORMAT, validate("10020a", "^\\d{6}$"))
    }

    @Test
    fun `a format the module cannot read does not block the code`() {
        assertEquals(CodeCheck.VALID, validate("100200", "([unbalanced"))
    }
}
