package dev.dacbiet.opusenclient.actions;

import dev.dacbiet.opusenclient.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get the card id of the card.
 */
public class GetCardIdAction extends Action {

    private static final Logger logger = LoggerFactory.getLogger(GetCardIdAction.class);
    private final FinishCallback onFinish;

    public GetCardIdAction(Client client, FinishCallback onFinish) {
        super(client);

        this.onFinish = onFinish;
    }

    @Override
    public boolean exec() throws ActionException {
        // there used to be more logic here...

        // after this action, client should have correct card id
        if (!new ConstructATRAction(this.client).exec()) {
            return false;
        }

        this.onFinish.run(this.client.getCardId());

        return true;
    }

    public interface FinishCallback {
        void run(String cardId);
    }
}
