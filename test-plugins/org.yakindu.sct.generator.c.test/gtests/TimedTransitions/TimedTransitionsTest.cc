/**
* Copyright (c) 2017 committers of YAKINDU and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     committers of YAKINDU - initial API and implementation
*/
#include "gtest/gtest.h"
#include "TimedTransitions.h"
#include "TimedTransitionsRequired.h"

static TimedTransitions statechart;

class StatemachineTest : public ::testing::Test{
	protected:
	virtual void SetUp() {
		timedTransitions_init(&statechart);
	}
};


TEST_F(StatemachineTest, Timer01) {					
	timedTransitions_enter(&statechart);
	EXPECT_TRUE(timedTransitions_isStateActive(&statechart, TimedTransitions_main_region_Start));
	EXPECT_TRUE(timedTransitions_isStateActive(&statechart, TimedTransitions_main_region_End));
}

void timedTransitions_setTimer(TimedTransitions* statechart, const sc_eventid evid, const sc_integer time_ms, const sc_boolean periodic){
	// Mockup
	timedTransitions_raiseTimeEvent(handle, evid);
}

void timedTransitions_unsetTimer(TimedTransitions* statechart, const sc_eventid evid){
	// Mockup
}		

