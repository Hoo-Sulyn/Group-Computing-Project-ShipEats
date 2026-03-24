const {onCall, HttpsError} = require("firebase-functions/v2/https");
const stripe = require("stripe")("sk_test_51T3SmJ0cHohjJvNLe1YIrvZXHgIMBsrxrFVN2PaErXGm4qB8LirkQZkjaZMc5fufNB8otS87oOwQRAB12GviAR6V00Wz9RGhp9");

exports.createStripePaymentIntent = onCall(async (request) => {
  // In v2, the data from Android is inside request.data
  const amount = request.data.amount;

  if (!amount) {
    console.error("No amount provided in request.data");
    throw new HttpsError("invalid-argument", "The function must be called with an 'amount'.");
  }

  try {
    const paymentIntent = await stripe.paymentIntents.create({
      amount: Math.round(amount),
      currency: "myr",
      automatic_payment_methods: {enabled: true},
    });

    return {
      clientSecret: paymentIntent.client_secret,
    };
  } catch (error) {
    console.error("Stripe Error:", error.message);
    throw new HttpsError("internal", error.message);
  }
});