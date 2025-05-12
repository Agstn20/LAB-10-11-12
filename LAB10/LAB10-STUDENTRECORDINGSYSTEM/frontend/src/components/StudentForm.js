import React, { useState } from "react";


const StudentForm = ({ addStudent }) => {
  const [name, setName] = useState("");
  const [course, setCourse] = useState("");
  const [message, setMessage] = useState("");


  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name || !course) {
      setMessage("Please fill in all fields.");
      return;
    }
    await addStudent(name, course); // Call function from App.js
    setName("");
    setCourse("");
    setMessage("Student added successfully!");
  };


  return (
    <div>
      <form onSubmit={handleSubmit}>
        <input type="text" placeholder="Student Name" value={name} onChange={(e) => setName(e.target.value)} />
        <input type="text" placeholder="Course" value={course} onChange={(e) => setCourse(e.target.value)} />
        <button type="submit">Add Student</button>
      </form>
      <p>{message}</p>
    </div>
  );
};


export default StudentForm;
