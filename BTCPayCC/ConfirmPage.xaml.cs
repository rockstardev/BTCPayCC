namespace BTCPayCC;

public partial class ConfirmPage : ContentPage
{
    private decimal _amount;

    public ConfirmPage(decimal amount)
    {
        InitializeComponent();
        _amount = amount;
        AmountLabel.Text = $"Charging ${_amount:F2}";
        // Here, trigger Stripe Tap-to-Pay logic
    }
}