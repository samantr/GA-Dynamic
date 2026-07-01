package com.gadynamicapp.itc.ga;

import com.gadynamicapp.itc.model.ClassRoomOption;
import com.gadynamicapp.itc.model.ClassTimeOption;
import com.gadynamicapp.itc.model.ItcClass;
import com.gadynamicapp.itc.model.ItcRoom;
import com.gadynamicapp.itc.model.RoomUnavailable;
import com.gadynamicapp.itc.parser.ParseResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RoomConflictRepairOperator implements ChromosomeRepairOperator {

    private static final int MAX_REPAIR_PASSES = 3;

    @Override
    public Chromosome repair(Chromosome chromosome, ParseResult problem) {
        if (chromosome.geneCount() != problem.classes().size()) {
            throw new IllegalArgumentException("Chromosome gene count must match problem class count.");
        }

        List<Gene> repairedGenes = new ArrayList<>(chromosome.genes());
        Map<String, ItcRoom> roomsById = roomsById(problem);

        for (int pass = 0; pass < MAX_REPAIR_PASSES; pass++) {
            boolean changed = false;

            for (int classIndex = 0; classIndex < repairedGenes.size(); classIndex++) {
                RepairCandidate candidate = findBestRepairCandidate(
                        classIndex,
                        repairedGenes,
                        problem,
                        roomsById
                );

                if (candidate != null) {
                    repairedGenes.set(classIndex, candidate.gene());
                    changed = true;
                }
            }

            if (!changed) {
                break;
            }
        }

        return new Chromosome(repairedGenes);
    }

    private RepairCandidate findBestRepairCandidate(
            int classIndex,
            List<Gene> genes,
            ParseResult problem,
            Map<String, ItcRoom> roomsById
    ) {
        ItcClass itcClass = problem.classes().get(classIndex);
        Gene currentGene = genes.get(classIndex);

        if (itcClass.roomOptions().isEmpty() || currentGene.roomOptionIndex() == Gene.NO_ROOM) {
            return null;
        }

        validateGene(itcClass, currentGene, classIndex);

        ClassTimeOption selectedTime = itcClass.timeOptions().get(currentGene.timeOptionIndex());
        ClassRoomOption currentRoom = itcClass.roomOptions().get(currentGene.roomOptionIndex());

        RoomHardCost currentCost = roomHardCostFor(
                classIndex,
                currentRoom.roomId(),
                selectedTime,
                genes,
                problem,
                roomsById
        );

        /*
         * This repair is intentionally targeted.
         * If the class is not involved in a room conflict, leave it unchanged.
         */
        if (currentCost.roomConflictCount() == 0) {
            return null;
        }

        RepairCandidate bestCandidate = null;

        for (int roomIndex = 0; roomIndex < itcClass.roomOptions().size(); roomIndex++) {
            if (roomIndex == currentGene.roomOptionIndex()) {
                continue;
            }

            ClassRoomOption candidateRoom = itcClass.roomOptions().get(roomIndex);

            RoomHardCost candidateCost = roomHardCostFor(
                    classIndex,
                    candidateRoom.roomId(),
                    selectedTime,
                    genes,
                    problem,
                    roomsById
            );

            if (candidateCost.total() >= currentCost.total()) {
                continue;
            }

            Gene candidateGene = new Gene(currentGene.timeOptionIndex(), roomIndex);

            RepairCandidate candidate = new RepairCandidate(
                    candidateGene,
                    candidateCost,
                    candidateRoom.penalty()
            );

            if (isBetterCandidate(candidate, bestCandidate)) {
                bestCandidate = candidate;
            }
        }

        return bestCandidate;
    }

    private boolean isBetterCandidate(RepairCandidate candidate, RepairCandidate bestCandidate) {
        if (bestCandidate == null) {
            return true;
        }

        if (candidate.cost().total() != bestCandidate.cost().total()) {
            return candidate.cost().total() < bestCandidate.cost().total();
        }

        if (candidate.cost().roomConflictCount() != bestCandidate.cost().roomConflictCount()) {
            return candidate.cost().roomConflictCount() < bestCandidate.cost().roomConflictCount();
        }

        if (candidate.cost().roomUnavailableCount() != bestCandidate.cost().roomUnavailableCount()) {
            return candidate.cost().roomUnavailableCount() < bestCandidate.cost().roomUnavailableCount();
        }

        return candidate.roomPenalty() < bestCandidate.roomPenalty();
    }

    private RoomHardCost roomHardCostFor(
            int classIndex,
            String roomId,
            ClassTimeOption selectedTime,
            List<Gene> genes,
            ParseResult problem,
            Map<String, ItcRoom> roomsById
    ) {
        int roomConflictCount = 0;

        for (int otherIndex = 0; otherIndex < genes.size(); otherIndex++) {
            if (otherIndex == classIndex) {
                continue;
            }

            ItcClass otherClass = problem.classes().get(otherIndex);
            Gene otherGene = genes.get(otherIndex);

            if (otherClass.roomOptions().isEmpty() || otherGene.roomOptionIndex() == Gene.NO_ROOM) {
                continue;
            }

            validateGene(otherClass, otherGene, otherIndex);

            ClassRoomOption otherRoom = otherClass.roomOptions().get(otherGene.roomOptionIndex());

            if (!roomId.equals(otherRoom.roomId())) {
                continue;
            }

            ClassTimeOption otherTime = otherClass.timeOptions().get(otherGene.timeOptionIndex());

            if (overlaps(selectedTime, otherTime)) {
                roomConflictCount++;
            }
        }

        int roomUnavailableCount = roomUnavailableCountFor(roomId, selectedTime, roomsById);

        return new RoomHardCost(roomConflictCount, roomUnavailableCount);
    }

    private int roomUnavailableCountFor(
            String roomId,
            ClassTimeOption selectedTime,
            Map<String, ItcRoom> roomsById
    ) {
        ItcRoom room = roomsById.get(roomId);

        if (room == null) {
            return 0;
        }

        int count = 0;

        for (RoomUnavailable unavailable : room.unavailableTimes()) {
            if (overlaps(selectedTime, unavailable)) {
                count++;
            }
        }

        return count;
    }

    private Map<String, ItcRoom> roomsById(ParseResult problem) {
        Map<String, ItcRoom> roomsById = new HashMap<>();

        for (ItcRoom room : problem.rooms()) {
            roomsById.put(room.id(), room);
        }

        return roomsById;
    }

    private void validateGene(ItcClass itcClass, Gene gene, int classIndex) {
        if (gene.timeOptionIndex() < 0 || gene.timeOptionIndex() >= itcClass.timeOptions().size()) {
            throw new IllegalArgumentException(
                    "Gene at index " + classIndex + " references an invalid time option."
            );
        }

        if (itcClass.roomOptions().isEmpty()) {
            if (gene.roomOptionIndex() != Gene.NO_ROOM) {
                throw new IllegalArgumentException(
                        "Gene at index " + classIndex + " references a room option, but the class has no room options."
                );
            }

            return;
        }

        if (gene.roomOptionIndex() < 0 || gene.roomOptionIndex() >= itcClass.roomOptions().size()) {
            throw new IllegalArgumentException(
                    "Gene at index " + classIndex + " references an invalid room option."
            );
        }
    }

    private boolean overlaps(ClassTimeOption left, ClassTimeOption right) {
        return bitstringsIntersect(left.days(), right.days())
                && bitstringsIntersect(left.weeks(), right.weeks())
                && intervalsOverlap(left.start(), left.length(), right.start(), right.length());
    }

    private boolean overlaps(ClassTimeOption time, RoomUnavailable unavailable) {
        return bitstringsIntersect(time.days(), unavailable.days())
                && bitstringsIntersect(time.weeks(), unavailable.weeks())
                && intervalsOverlap(time.start(), time.length(), unavailable.start(), unavailable.length());
    }

    private boolean bitstringsIntersect(String left, String right) {
        int length = Math.min(left.length(), right.length());

        for (int i = 0; i < length; i++) {
            if (left.charAt(i) == '1' && right.charAt(i) == '1') {
                return true;
            }
        }

        return false;
    }

    private boolean intervalsOverlap(int leftStart, int leftLength, int rightStart, int rightLength) {
        int leftEnd = leftStart + leftLength;
        int rightEnd = rightStart + rightLength;

        return leftStart < rightEnd && rightStart < leftEnd;
    }

    private record RoomHardCost(
            int roomConflictCount,
            int roomUnavailableCount
    ) {
        int total() {
            return roomConflictCount + roomUnavailableCount;
        }
    }

    private record RepairCandidate(
            Gene gene,
            RoomHardCost cost,
            int roomPenalty
    ) {
    }
}