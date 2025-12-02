package pos.services;

import pos.models.Transaction;

public class PromotionService {

    // Simple demo: FIX10 => PKR 10 off per transaction
    // PCT5 => 5% off subtotal
    public double applyPromo(Transaction tx, String code, double subtotal) {
        if (code == null) return 0.0;
        switch (code.toUpperCase()) {
            case "FIX10":
                tx.setPromoCode("FIX10");
                return 10.0;
            case "PCT5":
                tx.setPromoCode("PCT5");
                return subtotal * 0.05;
            default:
                return 0.0;
        }
    }
}
