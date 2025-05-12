import React from "react";

const StudentList = ({ students, deleteStudent }) => {
  return (
    <div className="text-center">
      <h2>Student List</h2>
      <ul>
        {students.map((student) => (
          <li key={student.id}>
            {student.image && <img src={`http://localhost:5000${student.image}`} alt="Student" width="50" />}
            <strong>{student.name}</strong> - {student.course}
            <button onClick={() => deleteStudent(student.id)}>Delete</button>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default StudentList;

