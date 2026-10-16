/** Application ring: coordinates use cases (loads aggregates, invokes their behaviour, saves them, publishes events) and marks the transaction boundary; depends only on the domain. */
@ApplicationRing
package de.jcss.hbt.application;

import org.jmolecules.architecture.onion.simplified.ApplicationRing;
