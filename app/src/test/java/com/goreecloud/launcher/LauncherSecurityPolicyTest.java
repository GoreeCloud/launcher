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
 @Test public void corruptPinCannotBeResetWhileAppsRemainLocked(){
  assertTrue(LauncherAppLock.permitsFreshPin(false,0));
  assertFalse(LauncherAppLock.permitsFreshPin(false,1));
  assertFalse(LauncherAppLock.permitsFreshPin(false,7));
  assertFalse(LauncherAppLock.permitsFreshPin(true,0));
 }
 @Test public void retryCooldownStartsAtFiveAndCapsAtThirtyMinutes(){
  assertEquals(0L,LauncherAppLock.cooldownDurationMillis(0));
  assertEquals(0L,LauncherAppLock.cooldownDurationMillis(4));
  assertEquals(60000L,LauncherAppLock.cooldownDurationMillis(5));
  assertEquals(120000L,LauncherAppLock.cooldownDurationMillis(6));
  assertEquals(1800000L,LauncherAppLock.cooldownDurationMillis(50));
 }
}
