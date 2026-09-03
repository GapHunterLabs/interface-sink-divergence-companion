import org.springframework.web.bind.annotation.PostMapping;

interface PaymentGateway {
    void charge(String cardToken);
}

class LoggingGateway implements PaymentGateway {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingGateway.class);
    public void charge(String cardToken) {
        log.info(cardToken);
    }
}

class QuietGateway implements PaymentGateway {
    public void charge(String cardToken) {
        // never logs
    }
}

class Checkout {
    @PostMapping("/pay")
    void handle(PaymentGateway gateway, String cardToken) {
        gateway.charge(cardToken);
    }
}
