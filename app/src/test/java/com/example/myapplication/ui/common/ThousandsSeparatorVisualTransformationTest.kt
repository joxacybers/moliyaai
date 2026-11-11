package com.example.myapplication.ui.common

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class ThousandsSeparatorVisualTransformationTest {

    private val transformation = ThousandsSeparatorVisualTransformation(' ')

    @Test
    fun formatsEmptyString_returnsEmpty() {
        val result = transformation.filter(AnnotatedString(""))
        assertEquals("", result.text.text)
    }

    @Test
    fun formatsThreeDigits_noSeparator() {
        val result = transformation.filter(AnnotatedString("123"))
        assertEquals("123", result.text.text)
    }

    @Test
    fun formatsFourDigits_hasSeparator() {
        val result = transformation.filter(AnnotatedString("1234"))
        assertEquals("1 234", result.text.text)
    }

    @Test
    fun formatsOneMillion_groupsInThrees() {
        val result = transformation.filter(AnnotatedString("1000000"))
        assertEquals("1 000 000", result.text.text)
    }

    @Test
    fun offsetMapping_transformsCorrectly() {
        val text = "1000000" // transformed: "1 000 000"
        val result = transformation.filter(AnnotatedString(text))

        // Index 0 ('1') -> transformed 0
        assertEquals(0, result.offsetMapping.originalToTransformed(0))
        // Index 1 (after '1') -> transformed 2 (after '1 ')
        assertEquals(2, result.offsetMapping.originalToTransformed(1))
        // Index 4 (after "1000") -> transformed 6 (after "1 000 ")
        assertEquals(6, result.offsetMapping.originalToTransformed(4))
        // Index 7 (end) -> transformed 9 ("1 000 000".length)
        assertEquals(9, result.offsetMapping.originalToTransformed(7))

        // Reverse: transformed to original
        assertEquals(0, result.offsetMapping.transformedToOriginal(0))
        assertEquals(1, result.offsetMapping.transformedToOriginal(1))
        assertEquals(1, result.offsetMapping.transformedToOriginal(2)) // Space maps back to original 1
        assertEquals(2, result.offsetMapping.transformedToOriginal(3))
        assertEquals(7, result.offsetMapping.transformedToOriginal(9))
    }
}
