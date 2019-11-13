package com.feinfone.apns;

import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.PushNotificationResponse;
import com.turo.pushy.apns.auth.ApnsSigningKey;
import com.turo.pushy.apns.util.ApnsPayloadBuilder;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import com.turo.pushy.apns.util.TokenUtil;
import com.turo.pushy.apns.util.concurrent.PushNotificationFuture;
import com.turo.pushy.apns.util.concurrent.PushNotificationResponseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

enum PushEnvironment {
    STAGE,
    PRODUCTION
}

public class PushManager {

    private static final Logger log = LoggerFactory.getLogger(PushManager.class);

    private ApnsClient pushClient = null;

    public PushManager(
            String pathToCertificate,
            String teamId,
            String keyId,
            PushEnvironment env) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        pushClient = new ApnsClientBuilder()
                .setApnsServer(hostForEnvironment(env))
                .setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File(pathToCertificate),
                        teamId, keyId))
                .build();
    }

    public void sendPush(String message, int badgeNumber, String soundName, String token) {
        final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
        payloadBuilder.setAlertBody("Example!")
        .setBadgeNumber(badgeNumber)
        .setSound(soundName);

        final String payload = payloadBuilder.buildWithDefaultMaximumLength();
        final String sanitizedToken = TokenUtil.sanitizeTokenString(token);

        SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(sanitizedToken,
                "com.example.myApp", payload);
        // TODO: look at the response
        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> pushFuture =
                pushClient.sendNotification(pushNotification);
        pushFuture.addListener(new PushNotificationResponseListener<SimpleApnsPushNotification>() {

            @Override
            public void operationComplete(final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> future) throws Exception {
                // When using a listener, callers should check for a failure to send a
                // notification by checking whether the future itself was successful
                // since an exception will not be thrown.
                if (future.isSuccess()) {
                    final PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse =
                            pushFuture.getNow();

                    log.info("Push message sent successfully");
                } else {
                    // Something went wrong when trying to send the notification to the
                    // APNs gateway. We can find the exception that caused the failure
                    // by getting future.cause().
                    log.error("Push message failed");
                    future.cause().printStackTrace();
                }
            }
        });
    }

    private String hostForEnvironment(PushEnvironment env) {
        switch (env) {
            case STAGE:
                return ApnsClientBuilder.DEVELOPMENT_APNS_HOST;
            case PRODUCTION:
                return ApnsClientBuilder.PRODUCTION_APNS_HOST;
            default:
                return null;
        }
    }
}
