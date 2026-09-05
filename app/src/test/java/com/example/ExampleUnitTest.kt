package com.example

import com.example.data.local.CallSender
import com.example.data.local.LiveCallTranscriptItem
import com.example.data.local.TutorCatalog
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testTutorCatalogPopulated() {
    val tutors = TutorCatalog.tutors
    assertTrue(tutors.isNotEmpty())
    val emma = tutors.first { it.id == "tutor_emma" }
    assertEquals("Emma Watson", emma.name)
    assertTrue(emma.defaultTopics.isNotEmpty())
  }

  @Test
  fun testLiveCallTranscriptItemCreation() {
    val item = LiveCallTranscriptItem(
      sender = CallSender.USER,
      text = "I would like to improve my English fluency."
    )
    assertEquals(CallSender.USER, item.sender)
    assertEquals("I would like to improve my English fluency.", item.text)
    assertNotNull(item.id)
  }
}
