Feature: Download endpoint
  As a James user
  I want to access to the download endpoint

  Background:
    Given a domain named "domain.tld"
    And some users "usera@domain.tld", "userb@domain.tld", "userc@domain.tld"
    And "usera@domain.tld" has a mailbox "INBOX"
    And "usera@domain.tld" mailbox "INBOX" contains a message "m1" with an attachment "a1"
    
  Scenario: An authenticated user should initiate the access to the download endpoint
    Given "usera@domain.tld" is connected
    When "usera@domain.tld" checks for the availability of the attachment endpoint
    Then the user should be authorized

  Scenario: An unauthenticated user should initiate the access to the download endpoint
    When "usera@domain.tld" checks for the availability of the attachment endpoint
    Then the user should be authorized

  Scenario: An unauthenticated user should not have access to the download endpoint
    When "usera@domain.tld" downloads "a1"
    Then the user should not be authorized

  Scenario: An authenticated user should not have access to the download endpoint without the authentication token
    Given "usera@domain.tld" is connected
    When "usera@domain.tld" downloads "a1" without the authentication token
    Then the user should not be authorized

  Scenario: An authenticated user should not have access to the download endpoint with an empty authentication token
    Given "usera@domain.tld" is connected
    When "usera@domain.tld" downloads "a1" with empty authentication token
    Then the user should not be authorized

  Scenario: An authenticated user should not have access to the download endpoint with a bad authentication token
    Given "usera@domain.tld" is connected
    When "usera@domain.tld" downloads "a1" with a bad authentication token
    Then the user should not be authorized

  Scenario: An authenticated user should not have access to the download endpoint with an unknown authentication token
    Given "usera@domain.tld" is connected
    When "usera@domain.tld" downloads "a1" with an invalid authentication token
    Then the user should not be authorized

  Scenario: An authenticated user should have access to the download endpoint
    Given "usera@domain.tld" is connected
    When "usera@domain.tld" downloads "a1"
    Then the user should be authorized

  Scenario: An authenticated user should not have access to the download endpoint when token has expired
    Given "usera@domain.tld" is connected
    When "usera@domain.tld" downloads "a1" with an expired token
    Then the user should not be authorized

  Scenario: An authenticated user should not have access to the download endpoint without a blobId
    Given "usera@domain.tld" is connected
    When "usera@domain.tld" downloads "a1" without blobId parameter
    Then the user should receive a bad request response

  Scenario: An authenticated user should not retrieve anything when using wrong blobId
    Given "usera@domain.tld" is connected
    When "usera@domain.tld" downloads "a1" with wrong blobId
    Then the user should receive a not found response

  @Ignore
  Scenario: An authenticated user should not have access to someone else attachment
    Given "userb@domain.tld" is connected
    When "userb@domain.tld" downloads "a1"
    Then the user should receive a not found response

  @Ignore
  Scenario: An authenticated user should have access to a shared attachment
    Given "usera@domain.tld" shares its mailbox "INBOX" with "userb@domain.tld"
    And "userb@domain.tld" is connected
    When "userb@domain.tld" downloads "a1"
    Then the user should be authorized
