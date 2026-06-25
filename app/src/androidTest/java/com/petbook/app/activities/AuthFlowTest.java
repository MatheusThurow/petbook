package com.petbook.app.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import com.google.firebase.auth.FirebaseAuth;
import com.petbook.app.R;
import com.petbook.app.models.User;
import com.petbook.app.repositories.FirebaseUserRepository;
import com.petbook.app.utils.UserProfileStorage;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AuthFlowTest {

    private static final String VALID_EMAIL = "univaliteste@gmail.com";
    private static final String VALID_PASSWORD = "Univali";
    private static final String INVALID_PASSWORD = "Univali12345";
    private static final String RESET_EMAIL = "ferreiraarthur2812@gmail.com";
    private static final long DEMO_PAUSE_MILLIS = 1200L;

    @Rule
    public ActivityTestRule<LoginActivity> loginRule =
            new ActivityTestRule<>(LoginActivity.class, true, false);

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        UserProfileStorage.clearProfile(context);
        FirebaseAuth.getInstance().signOut();

        FirebaseUserRepository.setTestDelegate(new FirebaseUserRepository.TestDelegate() {
            @Override
            public boolean handleAuthenticate(
                    Context context,
                    String email,
                    String password,
                    FirebaseUserRepository.UserCallback callback
            ) {
                if (VALID_EMAIL.equalsIgnoreCase(email) && VALID_PASSWORD.equals(password)) {
                    callback.onSuccess(createValidUser());
                } else {
                    callback.onError("Usuario ou senha invalidos.");
                }
                return true;
            }

            @Override
            public boolean handleSendPasswordResetEmail(
                    Context context,
                    String email,
                    FirebaseUserRepository.CompletionCallback callback
            ) {
                if (RESET_EMAIL.equalsIgnoreCase(email)) {
                    callback.onSuccess();
                } else {
                    callback.onError("Nao encontramos um usuario ativo com esse e-mail.");
                }
                return true;
            }
        });

        Intents.init();
    }

    @After
    public void tearDown() {
        Context context = ApplicationProvider.getApplicationContext();
        UserProfileStorage.clearProfile(context);
        FirebaseAuth.getInstance().signOut();
        FirebaseUserRepository.clearTestDelegate();
        Intents.release();
    }

    @Test
    public void autenticacaoComUsuarioESenhaValidosDirecionaParaHome() {
        intending(hasComponent(FeedActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        loginRule.launchActivity(null);
        pauseForDemo();

        onView(withId(R.id.editLoginEmail)).perform(replaceText(VALID_EMAIL), closeSoftKeyboard());
        pauseForDemo();
        onView(withId(R.id.editLoginPassword)).perform(replaceText(VALID_PASSWORD), closeSoftKeyboard());
        pauseForDemo();
        onView(withId(R.id.buttonLogin)).perform(click());
        pauseForDemo();

        intended(hasComponent(FeedActivity.class.getName()));

        Context context = ApplicationProvider.getApplicationContext();
        assertEquals(Long.valueOf(1L), UserProfileStorage.getUserId(context));
        assertEquals(VALID_EMAIL, UserProfileStorage.getEmail(context, ""));
    }

    @Test
    public void esqueciMinhaSenhaComEmailValidoExibeSucesso() {
        loginRule.launchActivity(null);
        pauseForDemo();

        onView(withId(R.id.textForgotPassword)).perform(click());
        pauseForDemo();
        onView(withId(R.id.editForgotPasswordEmail)).perform(replaceText(RESET_EMAIL), closeSoftKeyboard());
        pauseForDemo();
        onView(withId(R.id.editForgotPasswordConfirmEmail)).perform(replaceText(RESET_EMAIL), closeSoftKeyboard());
        pauseForDemo();
        onView(withId(R.id.buttonResetPassword)).perform(click());
        pauseForDemo();

        onView(withId(R.id.editLoginEmail)).check(matches(isDisplayed()));
    }

    @Test
    public void autenticacaoComSenhaErradaNaoDirecionaParaHomeEAvisaErro() {
        loginRule.launchActivity(null);
        pauseForDemo();

        onView(withId(R.id.editLoginEmail)).perform(replaceText(VALID_EMAIL), closeSoftKeyboard());
        pauseForDemo();
        onView(withId(R.id.editLoginPassword)).perform(replaceText(INVALID_PASSWORD), closeSoftKeyboard());
        pauseForDemo();
        onView(withId(R.id.buttonLogin)).perform(click());
        pauseForDemo();

        onView(withId(R.id.buttonLogin)).check(matches(isDisplayed()));
        onView(withId(R.id.editLoginEmail)).check(matches(isDisplayed()));

        Context context = ApplicationProvider.getApplicationContext();
        assertNull(UserProfileStorage.getUserId(context));
    }

    private User createValidUser() {
        return new User(
                1L,
                "PERSON",
                "Univali Teste",
                VALID_EMAIL,
                VALID_PASSWORD,
                "",
                true
        );
    }

    private void pauseForDemo() {
        SystemClock.sleep(DEMO_PAUSE_MILLIS);
    }
}
