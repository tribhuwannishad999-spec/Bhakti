const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_devotee_101";
const BOB_UID = "bob_devotee_202";
const ADMIN_UID = "owner_admin_999";
const OWNER_EMAIL = "Tribhuwannishad999@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msAgo = 5000) {
  return new Date(Date.now() - msAgo);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("donations").get());
  await assertFails(
    unauthDb.collection("kathas").where("published", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Guest",
      language: "hi",
      punyaScore: 10,
      totalChants: 1,
      sadhanaLevel: 1,
      bestScore: 10,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("UserProfile: owner can create and update progress; cross-user and extra keys rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "राम भक्त",
      language: "hi",
      punyaScore: 50,
      totalChants: 5,
      sadhanaLevel: 1,
      bestScore: 50,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid partial update preserving createdAt and userId
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      punyaScore: 120,
      totalChants: 12,
      sadhanaLevel: 2,
      bestScore: 120,
      updatedAt: pastDate(1000),
    })
  );

  // Reject future timestamp tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      punyaScore: 200,
      updatedAt: new Date(Date.now() + 120000),
    })
  );

  // Reject shadow field injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );

  // Reject cross-user read and update
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: pastDate(500),
    })
  );
});

test("User favorites subcollection: isolated strictly to owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("favorites")
      .doc("fav_bhajan_1")
      .set({
        itemId: "bhajan_1",
        itemType: "bhajan",
        title: "श्री हनुमान चालीसा",
        createdAt: pastDate(),
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("favorites").get()
  );
});

test("Donations: donor can only create PENDING_VERIFICATION and never fake VERIFIED_SUCCESS", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ownerDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: OWNER_EMAIL,
      email_verified: true,
    })
    .firestore();
  const spoofedOwnerDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: OWNER_EMAIL,
      email_verified: false,
    })
    .firestore();

  const created = pastDate(10000);

  // Forging VERIFIED_SUCCESS as a regular donor must FAIL
  await assertFails(
    aliceDb.collection("donations").doc("don_fake_1").set({
      userId: ALICE_UID,
      donorName: "Alice",
      amountInr: 101,
      purpose: "मंदिर सेवा",
      paymentApp: "Google Pay",
      transactionRef: "UTR123456789",
      status: "VERIFIED_SUCCESS",
      receiptNumber: "BD-2026-0001",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid PENDING_VERIFICATION donation succeeds
  await assertSucceeds(
    aliceDb.collection("donations").doc("don_valid_1").set({
      userId: ALICE_UID,
      donorName: "आदित्य शर्मा",
      donorPhone: "9876543210",
      amountInr: 101,
      purpose: "गौ सेवा एवं मंदिर संरक्षण",
      paymentApp: "PhonePe",
      transactionRef: "UTR9988776655",
      status: "PENDING_VERIFICATION",
      receiptNumber: "BD-2026-0002",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Donor can list ONLY their own donations via filtered query
  await assertSucceeds(
    aliceDb.collection("donations").where("userId", "==", ALICE_UID).get()
  );
  // Unfiltered donation list fails for regular user
  await assertFails(aliceDb.collection("donations").get());
  // Cross-user PII read fails
  await assertFails(bobDb.collection("donations").doc("don_valid_1").get());
  // Donor cannot self-verify
  await assertFails(
    aliceDb.collection("donations").doc("don_valid_1").update({
      status: "VERIFIED_SUCCESS",
      updatedAt: pastDate(1000),
    })
  );
  // Unverified spoofed owner email CANNOT verify donation
  await assertFails(
    spoofedOwnerDb.collection("donations").doc("don_valid_1").update({
      status: "VERIFIED_SUCCESS",
      ownerNote: "Spoofed",
      updatedAt: pastDate(1000),
    })
  );
  // Verified owner CAN verify donation and list all donations
  await assertSucceeds(ownerDb.collection("donations").get());
  await assertSucceeds(
    ownerDb.collection("donations").doc("don_valid_1").update({
      status: "VERIFIED_SUCCESS",
      ownerNote: "UPI UTR bank statement se verify hua",
      updatedAt: pastDate(1000),
    })
  );
  // Owner cannot mutate immutable amountInr during verification
  await assertFails(
    ownerDb.collection("donations").doc("don_valid_1").update({
      amountInr: 99999,
      status: "VERIFIED_SUCCESS",
      updatedAt: pastDate(500),
    })
  );
});

test("Owner-managed Katha, Temple, Festival, Bhajan, MantraAudio collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ownerDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: OWNER_EMAIL,
      email_verified: true,
    })
    .firestore();
  const created = pastDate(10000);

  // Non-admin cannot create katha
  await assertFails(
    aliceDb.collection("kathas").doc("katha_1").set({
      title: "शिव महापुराण कथा",
      description: "पावन शिव कथा श्रवण",
      category: "शिव कथा",
      videoUrl: "https://www.youtube.com/watch?v=example1",
      duration: "24:10",
      published: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Verified owner can create katha and mantra audio
  await assertSucceeds(
    ownerDb.collection("kathas").doc("katha_1").set({
      title: "शिव महापुराण कथा",
      description: "पावन शिव कथा श्रवण",
      category: "शिव कथा",
      videoUrl: "https://www.youtube.com/watch?v=example1",
      duration: "24:10",
      speaker: "आचार्य जी",
      published: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    ownerDb.collection("mantra_audio").doc("om_namah_shivaya").set({
      mantraText: "ॐ नमः शिवाय",
      audioUrl: "https://example.com/audio/om_namah_shivaya.mp3",
      voiceLabel: "Owner Studio Recording",
      customRecording: true,
      published: true,
      updatedAt: created,
    })
  );

  // Signed-in user can query published kathas & mantra_audio
  await assertSucceeds(
    aliceDb.collection("kathas").where("published", "==", true).get()
  );
  await assertSucceeds(
    aliceDb.collection("mantra_audio").where("published", "==", true).get()
  );
  // Unfiltered query on kathas fails for non-admin
  await assertFails(aliceDb.collection("kathas").get());
});
