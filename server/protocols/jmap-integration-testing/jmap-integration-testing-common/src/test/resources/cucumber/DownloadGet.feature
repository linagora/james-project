Feature: Download GET
  As a James user
  I want to retrieve my blobs (attachments and messages)

  Background:
    Given a domain named "domain.tld"
    And a connected user "alice@domain.tld"
    And "alice@domain.tld" has a mailbox "INBOX"
    And "alice@domain.tld" has a mailbox "sharedMailbox"

  Scenario: Getting an attachment previously stored
    Given "alice@domain.tld" mailbox "INBOX" contains a message "1" with an attachment "2"
    When "alice@domain.tld" downloads "2"
    Then the user should receive that blob
    And the blob size is 3071

  Scenario: Getting an attachment with an unknown blobId
    Given "alice@domain.tld" mailbox "INBOX" contains a message "1" with an attachment "2"
    When "alice@domain.tld" downloads "2" with a valid authentication token but a bad blobId
    Then the user should receive a not found response

  Scenario: Getting an attachment previously stored with a desired name
    Given "alice@domain.tld" mailbox "INBOX" contains a message "1" with an attachment "2"
    When "alice@domain.tld" downloads "2" with "myFileName.txt" name
    Then the user should receive that blob
    And the attachment is named "myFileName.txt"

  Scenario: Getting an attachment previously stored with a non ASCII name
    Given "alice@domain.tld" mailbox "INBOX" contains a message "1" with an attachment "2"
    When "alice@domain.tld" downloads "2" with "ديناصور.odt" name
    Then the user should receive that blob
    And the attachment is named "ديناصور.odt"

  Scenario: Getting a message blob previously stored
    Given "alice@domain.tld" mailbox "INBOX" contains a message "1"
    When "alice@domain.tld" downloads "1"
    Then the user should receive that blob
    And the blob size is 4963

  Scenario: Getting a message then getting its blob
    Given the user has a message "m1" in "INBOX" mailbox with subject "my test subject", content "testmail"
    And the user ask for messages "m1"
    When "alice@domain.tld" downloads the message by its blobId
    Then the user should receive that blob
    And the blob size is 36

  Scenario: Deleted message should revoke attachment blob download rights
    Given "alice@domain.tld" mailbox "INBOX" contains a message "1" with an attachment "2"
    And "alice@domain.tld" delete mailbox "INBOX"
    When "alice@domain.tld" downloads "2"
    Then the user should receive a not found response

  Scenario: User cannot download attachment of another user
    Given "alice@domain.tld" mailbox "INBOX" contains a message "1" with an attachment "2"
    And a connected user "bob@domain.tld"
    And "bob@domain.tld" has a mailbox "INBOX"
    When "bob@domain.tld" downloads "2"
    Then the user should receive a not found response

  Scenario: User cannot download message blob of another user
    Given "alice@domain.tld" mailbox "INBOX" contains a message "1" with an attachment "2"
    And a connected user "bob@domain.tld"
    And "bob@domain.tld" has a mailbox "INBOX"
    When "bob@domain.tld" downloads "1"
    Then the user should receive a not found response

  Scenario: User can download attachment of another user when shared mailbox
    Given "alice@domain.tld" mailbox "sharedMailbox" contains a message "1" with an attachment "2"
    And "alice@domain.tld" shares its mailbox "sharedMailbox" with "bob@domain.tld"
    And a connected user "bob@domain.tld"
    And "bob@domain.tld" has a mailbox "sharedMailbox"
    When "bob@domain.tld" downloads "2"
    Then the user should receive that blob
    And the blob size is 3071

  Scenario: User can download message blob of another user when shared mailbox
    Given "alice@domain.tld" mailbox "sharedMailbox" contains a message "1" with an attachment "2"
    And "alice@domain.tld" shares its mailbox "sharedMailbox" with "bob@domain.tld"
    And a connected user "bob@domain.tld"
    And "bob@domain.tld" has a mailbox "sharedMailbox"
    When "bob@domain.tld" downloads "1"
    Then the user should receive that blob
    And the blob size is 4963