namespace BTCPayCC;

public partial class MainPage : ContentPage
{
    private string _amount = "";

    public MainPage()
    {
        InitializeComponent();
    }

    private void OnKeypadButtonClicked(object sender, EventArgs e)
    {
        if (sender is Button button)
        {
            _amount += button.Text;
            UpdateDisplay();
        }
    }

    private void OnClearClicked(object sender, EventArgs e)
    {
        _amount = "";
        UpdateDisplay();
    }

    private void OnConfirmClicked(object sender, EventArgs e)
    {
        if (decimal.TryParse(_amount, out var parsedAmount))
        {
            Navigation.PushAsync(new ConfirmPage(parsedAmount / 100)); // Convert cents to dollars
        }
    }

    private void UpdateDisplay()
    {
        if (string.IsNullOrWhiteSpace(_amount))
        {
            AmountLabel.Text = "$0";
            return;
        }

        var cents = int.Parse(_amount);
        AmountLabel.Text = $"${cents / 100}.{cents % 100:D2}";
    }
}