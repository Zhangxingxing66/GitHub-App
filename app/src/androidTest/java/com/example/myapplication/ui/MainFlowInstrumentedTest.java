package com.example.myapplication.ui;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

/**
 * 首页主流程的 Espresso 测试。
 * 这类测试运行在设备/模拟器上，验证真实 Activity、控件点击和页面跳转。
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainFlowInstrumentedTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void searchAndOpenFavorites_flowWorks() {
        // 输入搜索词并点击搜索，验证首页列表仍然可见。
        onView(withId(R.id.etSearch))
                .perform(replaceText("okhttp"), closeSoftKeyboard());
        onView(withId(R.id.btnSearch)).perform(click());
        onView(withId(R.id.recyclerRepos)).check(matches(isDisplayed()));

        // 点击收藏入口，验证成功进入收藏页并显示标题。
        onView(withId(R.id.btnFavorites)).perform(click());
        onView(withId(R.id.favoriteToolbar)).check(matches(isDisplayed()));
        onView(withText(R.string.favorite_title)).check(matches(isDisplayed()));
    }
}
