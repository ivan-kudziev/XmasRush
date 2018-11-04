package com.codingame.game.InputActions;

public abstract class Action {
    /**
     * The type of an input action.
     */
    public enum Type {
        PUSH(0),
        MOVE(1);

        /**
         * Integer to be used when converting the Type enum.
         */
        private int value;

        /**
         * Create a Type enum from an integer.
         * @param value the integer value.
         */
        Type(int value) {
            this.value = value;
        }

        /**
         * @return the Type enum as an integer.
         */
        public int getValue() {
            return value;
        }
    }
}
