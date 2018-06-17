package pum.dolinski_kawka.projectpum;

public interface GadgetListener {
    default void onTestReceived(boolean isDevOk) {
    }

    default void onIDReceived(String name, int id) {
    }

    default void onBatteryReceived(int state) {
    }

    default void onOnlineRegistrationStarted(OnlineSettings settings) {
    }

    default void onOnlineRegistrationStopped() {
    }

    default void onOnlineRegistrationSampleReceived(OnlineSample sample) {
    }
}
