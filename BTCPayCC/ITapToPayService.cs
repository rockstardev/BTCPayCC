namespace BTCPayCC;

public interface ITapToPayService
{
    Task StartPaymentAsync(decimal amount);
}
