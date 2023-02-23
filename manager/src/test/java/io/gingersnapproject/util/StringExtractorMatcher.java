package io.gingersnapproject.util;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class StringExtractorMatcher extends TypeSafeMatcher<String> {

   private String result;

   @Override
   protected boolean matchesSafely(String item) {
      result = item;
      return true;
   }

   @Override
   public void describeTo(Description description) {
      description.appendText("When applied put the returned String into the result object");
   }

   public String result() {
      return result;
   }
}
