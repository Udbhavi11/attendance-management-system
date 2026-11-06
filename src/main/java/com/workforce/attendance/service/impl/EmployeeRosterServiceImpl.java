
package com.workforce.attendance.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workforce.attendance.dto.EmployeeRosterDTO;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.EmployeeRoster;
import com.workforce.attendance.entity.Shift;
import com.workforce.attendance.exception.DuplicateResourceException;
import com.workforce.attendance.exception.ResourceNotFoundException;
import com.workforce.attendance.repository.EmployeeRepository;
import com.workforce.attendance.repository.EmployeeRosterRepository;
import com.workforce.attendance.repository.ShiftRepository;
import com.workforce.attendance.service.EmployeeRosterService;

@Service
@Transactional
public class EmployeeRosterServiceImpl implements EmployeeRosterService {

	private final EmployeeRosterRepository rosterRepository;
	private final EmployeeRepository employeeRepository;
	private final ShiftRepository shiftRepository;

	public EmployeeRosterServiceImpl(
			EmployeeRosterRepository rosterRepository,
			EmployeeRepository employeeRepository,
			ShiftRepository shiftRepository) {

		this.rosterRepository = rosterRepository;
		this.employeeRepository = employeeRepository;
		this.shiftRepository = shiftRepository;
	}

	@Override
	public EmployeeRosterDTO createRoster(EmployeeRosterDTO dto) {

		Employee employee = getEmployee(dto.getEmployeeId());

		validateEmployee(employee);

		validateRosterRules(dto);

		if (rosterRepository.existsByEmployeeAndRosterDate(
				employee, dto.getRosterDate())) {

			throw new DuplicateResourceException(
					"Roster already exists for this employee on this date"
					);
		}

		Shift shift = getShift(dto);

		EmployeeRoster roster = new EmployeeRoster();

		roster.setEmployee(employee);
		roster.setShift(shift);
		roster.setRosterDate(dto.getRosterDate());
		roster.setWeeklyOff(dto.isWeeklyOff());

		EmployeeRoster savedRoster = rosterRepository.save(roster);

		return convertToDTO(savedRoster);
	}



	@Override
	@Transactional(readOnly = true)
	public List<EmployeeRosterDTO> getAllRosters() {

		return rosterRepository.findAll()
				.stream()
				.map(this::convertToDTO)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public EmployeeRosterDTO getRosterById(Long id) {

		EmployeeRoster roster = rosterRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Roster not found with ID: " + id
						));

		return convertToDTO(roster);
	}


	@Override
	public EmployeeRosterDTO updateRoster(Long id, EmployeeRosterDTO dto) {

		EmployeeRoster roster = rosterRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Roster not found with ID: " + id
						));

		Employee employee = getEmployee(dto.getEmployeeId());

		validateEmployee(employee);
		validateRosterRules(dto);

		if (rosterRepository.existsByEmployeeAndRosterDate(
				employee, dto.getRosterDate())
				&& (!roster.getEmployee().getId().equals(employee.getId())
						|| !roster.getRosterDate().equals(dto.getRosterDate()))) {

			throw new DuplicateResourceException(
					"Roster already exists for this employee on this date"
					);
		}

		Shift shift = getShift(dto);

		roster.setEmployee(employee);
		roster.setShift(shift);
		roster.setRosterDate(dto.getRosterDate());
		roster.setWeeklyOff(dto.isWeeklyOff());

		EmployeeRoster updatedRoster = rosterRepository.save(roster);

		return convertToDTO(updatedRoster);
	}



	@Override
	@Transactional(readOnly = true)
	public List<EmployeeRosterDTO> getEmployeeRoster(Long employeeId) {

		Employee employee = getEmployee(employeeId);

		return rosterRepository
				.findByEmployeeOrderByRosterDateDesc(employee)
				.stream()
				.map(this::convertToDTO)
				.toList();
	}



	@Override
	public List<EmployeeRosterDTO> createBulkRoster(
			List<EmployeeRosterDTO> rosters) {

		if (rosters == null || rosters.isEmpty()) {
			throw new IllegalArgumentException(
					"Roster list cannot be empty"
					);
		}

		return rosters.stream()
				.map(dto -> this.createRoster(dto))
				.toList();
	}


	//Retrieve employee
	private Employee getEmployee(Long employeeId) {

		return employeeRepository.findById(employeeId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Employee not found with ID: " + employeeId
						));
	}

	//Validate employee
	private void validateEmployee(Employee employee) {

		if (!employee.isActive()) {
			throw new IllegalArgumentException(
					"Inactive employees cannot be assigned rosters"
					);
		}
	}

	//Validate weekly-off and shift rules
	private void validateRosterRules(EmployeeRosterDTO dto) {

		if (dto.isWeeklyOff() && dto.getShiftId() != null) {
			throw new IllegalArgumentException(
					"Weekly off cannot have an assigned shift"
					);
		}

		if (!dto.isWeeklyOff() && dto.getShiftId() == null) {
			throw new IllegalArgumentException(
					"A shift must be assigned for a working day"
					);
		}
	}

	//Retrieve and validate shift
	private Shift getShift(EmployeeRosterDTO dto) {

		if (dto.isWeeklyOff()) {
			return null;
		}

		Shift shift = shiftRepository.findById(dto.getShiftId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Shift not found with ID: " + dto.getShiftId()
						));

		if (!shift.isActive()) {
			throw new IllegalArgumentException(
					"Inactive shifts cannot be assigned"
					);
		}

		return shift;
	}


	private EmployeeRosterDTO convertToDTO(EmployeeRoster roster) {

		Long shiftId = roster.getShift() != null
				? roster.getShift().getId()
						: null;

		return new EmployeeRosterDTO(
				roster.getId(),
				roster.getEmployee().getId(),
				shiftId,
				roster.getRosterDate(),
				roster.isWeeklyOff()
				);
	}



}
