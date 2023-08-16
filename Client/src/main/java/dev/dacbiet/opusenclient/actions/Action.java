package dev.dacbiet.opusenclient.actions;

import dev.dacbiet.opusenclient.Client;

/**
 * Represent action to perform.
 */
public abstract class Action {

    public static final String TERMINAL_NAME = "Neowave LinkeoSIM Reader 0";

    protected final Client client;

    public Action(Client client) {
        this.client = client;
    }

    /**
     * Executes the action.
     * If returns false, then it failed to execute the action, but it can be re-executed.
     *
     * @return true if successfully executed action
     * @throws ActionException execution exception
     */
    public abstract boolean exec() throws ActionException;
}
