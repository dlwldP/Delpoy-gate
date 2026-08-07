package deploygate.security;

public enum CallerType {

    /**
     * A personal token belonging to one deployer. The caller's identity is proven,
     * so this is the only kind of caller allowed to approve or reject.
     */
    DEPLOYER,

    /**
     * The shared CI service token. The pipeline authenticates the human on its own
     * side (e.g. github.actor) and relays that name to us, so this caller may ask
     * about any deployer — but may never cast an approval vote as one.
     */
    CI_SERVICE
}
