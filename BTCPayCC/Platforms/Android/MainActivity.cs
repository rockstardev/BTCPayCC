using Android.App;
using Android.Content.PM;
using Android.OS;

namespace BTCPayCC;

[Activity(Theme = "@style/Maui.SplashTheme", MainLauncher = true, LaunchMode = LaunchMode.SingleTop,
    ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation | ConfigChanges.UiMode |
                           ConfigChanges.ScreenLayout | ConfigChanges.SmallestScreenSize | ConfigChanges.Density)]
public class MainActivity : MauiAppCompatActivity
{
    private TapToPayHandler _handler;

    protected override void OnCreate(Bundle savedInstanceState)
    {
        base.OnCreate(savedInstanceState);
        _handler = new TapToPayHandler(this, this);
    }

    public void StartTapToPay(long amountInCents)
    {
        _handler.StartPayment(amountInCents);
    }
}

public class TapToPayService : ITapToPayService
{
    public async Task StartPaymentAsync(decimal amount)
    {
        var activity = Platform.CurrentActivity as MainActivity;
        activity?.RunOnUiThread(() =>
        {
            activity.StartTapToPay((long)(amount * 100)); // cents
        });
    }
}