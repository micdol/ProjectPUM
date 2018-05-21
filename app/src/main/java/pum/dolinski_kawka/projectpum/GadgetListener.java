package pum.dolinski_kawka.projectpum;

public interface GadgetListener {
    void onIDReceived(String name, int id);
    void onBatteryReceived(int state);
    void onOnlineRejestrationSampleReceived(OnlineSample sample);
}
