package com.goreecloud.launcher;
import org.junit.Test;
import static org.junit.Assert.*;
public class LauncherSecurityPolicyTest {
 @Test public void pinPolicy(){
  assertTrue(LauncherAppLock.validPin("1234"));
  assertTrue(LauncherAppLock.validPin("123456"));
  assertFalse(LauncherAppLock.validPin("123"));
  assertFalse(LauncherAppLock.validPin("12a4"));
 }
}
