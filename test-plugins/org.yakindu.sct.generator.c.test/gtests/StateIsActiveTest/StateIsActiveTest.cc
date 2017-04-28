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
#include "StateIsActive.h"

StateIsActive handle;

class StatemachineTest : public ::testing::Test{
	protected:
	virtual void SetUp() {
		stateIsActive_init(&handle);
	}
};

TEST_F(StatemachineTest, stateIsActive) {					
	stateIsActive_enter(&handle);
	EXPECT_TRUE(stateIsActive_isStateActive(&handle, StateIsActive_R1_R1A));
	EXPECT_TRUE(stateIsActive_isStateActive(&handle, StateIsActive_R2_R2A));
	stateIsActiveIface_raise_event1(&handle);
	stateIsActive_runCycle(&handle);
	stateIsActive_runCycle(&handle);
	EXPECT_TRUE(stateIsActive_isStateActive(&handle, StateIsActive_R1_R1B));
	EXPECT_TRUE(stateIsActive_isStateActive(&handle, StateIsActive_R2_R2B));
}


