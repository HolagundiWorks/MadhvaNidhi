# Play Console data-safety draft

This is a preparation aid; the developer remains responsible for confirming the
declarations against the exact production artifact.

- Does the app collect or share required user-data types? **No**, based on the
  1.0.0 source and dependencies.
- Is all user data encrypted in transit? **Yes** for network requests; cleartext
  traffic is disabled and the official Panchanga source uses HTTPS.
- Account creation: **No account**.
- Data deletion request: **Not applicable to server data**. Local data is deleted
  through Android's Clear storage action or uninstall.
- Ads: **No**.
- Precise or approximate location: **Not collected**.
- App activity / analytics / crash reporting: **Not collected**.
- Personal and financial information: **Not collected**.

Standard server logs produced by `srsmatha.org` are governed by that site's own
practices. The app sends a date query and a non-identifying MadhvaNidhi user agent.

## Individual-publisher review note

The app does store personal Sadhak profile fields locally; “not collected” in
this draft refers to off-device collection, not absence of local profile data.
Confirm the exact signed build, network requests and third-party Panchanga
source handling before submitting final Console answers. Having no developer
backend alone does not establish that every collection/sharing answer is No.
The matching policy draft is `privacy-policy.md`; its public hosting URL is
still required.
