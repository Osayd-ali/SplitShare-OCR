import React, { useState } from "react";
import axios from "axios";

function ExtractDirectly() {
  const [file, setFile] = useState(null);
  const [userId, setUserId] = useState(""); 
  const [response, setResponse] = useState(null);
  const [error, setError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!file || !userId) {
      setError("Please provide a file and user ID.");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("userId", userId);

    try {
      const res = await axios.post("http://localhost:8080/api/receipts/extract", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });
      setResponse(res.data);
      setError("");
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.error || "Failed to extract receipt.");
      setResponse(null);
    }
  };

  return (
    <div>
      <h2>Extract Receipt Data</h2>
      <form onSubmit={handleSubmit}>
        <input
          type="file"
          onChange={(e) => setFile(e.target.files[0])}
          accept="image/*"
        />
        <br />
        <input
          type="text"
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
          placeholder="Enter User ID"
        />
        <br />
        <button type="submit">Extract</button>
      </form>

      {/* Test Get Receipt Button - appears after successful extract */}
      {response?.receiptId && (
        <button
          type="button"
          onClick={async () => {
            try {
              const res = await axios.get(
                `http://localhost:8080/api/receipts/${userId}/${response.receiptId}`
              );
              alert("Raw Receipt Text:\n" + res.data);
            } catch (err) {
              console.error(err);
              setError("Failed to fetch receipt text.");
            }
          }}
        >
          Test Get Receipt
        </button>
      )}

      {error && <p style={{ color: "red" }}>{error}</p>}

      {response && (
        <div>
          <h3>Extracted Receipt</h3>
          <pre>{JSON.stringify(response, null, 2)}</pre>
        </div>
      )}
    </div>
  );

}

export default ExtractDirectly;
