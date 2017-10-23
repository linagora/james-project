#***************************************************************
# Licensed to the Apache Software Foundation (ASF) under one   *
# or more contributor license agreements.  See the NOTICE file *
# distributed with this work for additional information        *
# regarding copyright ownership.  The ASF licenses this file   *
# to you under the Apache License, Version 2.0 (the            *
# "License"); you may not use this file except in compliance   *
# with the License.  You may obtain a copy of the License at   *
#                                                              *
#   http://www.apache.org/licenses/LICENSE-2.0                 *
#                                                              *
# Unless required by applicable law or agreed to in writing,   *
# software distributed under the License is distributed on an  *
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
# KIND, either express or implied.  See the License for the    *
# specific language governing permissions and limitations      *
# under the License.                                           *
# **************************************************************/
Feature: SetMessages method on shared folders
  As a James user
  I want to be able to modify properties of a shared mail

  Background:
    Given a domain named "domain.tld"
    And "bob@domain.tld" has a mailbox "shared"
    And "alice@domain.tld" has a mailbox "INBOX"
    And a user "bob@domain.tld"
    And "bob@domain.tld" has a message "mBob" in "shared" mailbox with two attachments in text
    And a connected user "alice@domain.tld"
    And "alice@domain.tld" has a message "mAlice" in "INBOX" mailbox with two attachments in text

# Set in mailboxes

  Scenario: SetMessages can copy messages from shared mailbox when allowed
    Given "bob@domain.tld" shares its mailbox "shared" with "alice@domain.tld" with rights "lr"
    And "alice@domain.tld" copy "mBob" from mailbox "shared" of user "bob@domain.tld" to mailbox "INBOX" of user "alice@domain.tld"
    When "alice@domain.tld" ask for messages "mBob"
    Then the list should contain 1 message
    And the id of the message is "mBob"
    And the message is in following user mailboxes:
        |alice@domain.tld |INBOX  |
        |bob@domain.tld   |shared |

  Scenario: SetMessages can move messages out of shared mailbox when allowed
    Given "bob@domain.tld" shares its mailbox "shared" with "alice@domain.tld" with rights "lrte"
    And "alice@domain.tld" move "mBob" to mailbox "INBOX" of user "alice@domain.tld"
    When "alice@domain.tld" ask for messages "mBob"
    Then the list should contain 1 message
    And the id of the message is "mBob"
    And the message is in following user mailboxes:
        |alice@domain.tld |INBOX  |

  Scenario: SetMessages can add messages to a shared mailbox when allowed
    Given "bob@domain.tld" shares its mailbox "shared" with "alice@domain.tld" with rights "lri"
    And "alice@domain.tld" copy "mAlice" from mailbox "INBOX" of user "alice@domain.tld" to mailbox "shared" of user "bob@domain.tld"
    When "alice@domain.tld" ask for messages "mAlice"
    Then the list should contain 1 message
    And the id of the message is "mAlice"
#    And the message is in "alice@domain.tld:INBOX,bob@domain.tld:shared" user mailboxes
    And the message is in following user mailboxes:
        |alice@domain.tld |INBOX  |
        |bob@domain.tld   |shared |

  Scenario: SetMessages can not copy messages from shared mailbox when not allowed
    Given "bob@domain.tld" shares its mailbox "shared" with "alice@domain.tld" with rights "litewsa"
    When "alice@domain.tld" copy "mBob" from mailbox "shared" of user "bob@domain.tld" to mailbox "INBOX" of user "alice@domain.tld"
    Then the message "mBob" is not updated

  Scenario: SetMessages can not copy messages to shared mailbox when not allowed
    Given "bob@domain.tld" shares its mailbox "shared" with "alice@domain.tld" with rights "lrtewsa"
    When "alice@domain.tld" copy "mAlice" from mailbox "INBOX" of user "alice@domain.tld" to mailbox "shared" of user "bob@domain.tld"
    Then the message "mAlice" is not updated

  Scenario: SetMessages can not move messages out of shared mailbox when not allowed
    Given "bob@domain.tld" shares its mailbox "shared" with "alice@domain.tld" with rights "lriwsa"
    When "alice@domain.tld" move "mBob" to mailbox "INBOX" of user "alice@domain.tld"
    Then the message "mBob" is not updated

# Flags update

  Scenario: SetMessages can update delegated message flags when allowed
    Given "bob@domain.tld" shares its mailbox "shared" with "alice@domain.tld" with rights "lrw"
    When "alice@domain.tld" set flags on "mBob" to "$Flagged"
    When "alice@domain.tld" ask for messages "mBob"
    Then the list should contain 1 message
    And the id of the message is "mBob"
    And the keywords of the message is $Flagged


  Scenario: SetMessages can not update delegated message flags when not allowed
    Given "bob@domain.tld" shares its mailbox "shared" with "alice@domain.tld" with rights "latires"
    When "alice@domain.tld" set flags on "mBob" to "$Flagged"
    Then the message "mBob" is not updated